# Reconstruction complète — findMe `auth-service`

> Source de vérité unique : `antigravity-prompt-auth-service.md`
> La collection Postman est exclue de l'analyse.

---

## Contexte

Le code actuel (`com.example.findme`) est une version simplifiée qui ne respecte pas l'architecture
exigée par le prompt. Il sera **entièrement supprimé et remplacé** par une implémentation conforme :

| Élément | État actuel | Cible |
|---|---|---|
| Package racine | `com.example.findme` | `com.geolink.findme.authservice` |
| RBAC | Enum `Role` en champ `User` | RBAC dynamique : tables `roles`, `permissions`, `user_roles`, `role_permissions` |
| Refresh tokens | Absent | `RefreshToken` entité + rotation + hash SHA-256 |
| PasswordResetToken | Absent | Entité + token hashé + endpoints `forgot/reset-password` |
| Architecture | Service + Controller mélangés | Hexagonale en couches : `entity / repository / security / service / filter / config / controller / dto / exception` |
| Tests | Partiels, dépendent du contexte Spring | Unitaires Mockito (no Spring) + Intégration Testcontainers |

---

## Workflow 0 — Contrats d'API (source : prompt §4 et §6 uniquement)

### Contrats figés (§4 du prompt)

| Méthode | Route | Auth | Code succès | Codes erreur |
|---|---|---|---|---|
| POST | `/api/auth/signup` | non | 201 | 400, 409 |
| POST | `/api/auth/signin` | non | 200 | 401 (message générique) |
| POST | `/api/auth/refresh` | non | 200 (rotation) | 401 |
| POST | `/api/auth/logout` | oui | 204 | 401 |
| POST | `/api/auth/forgot-password` | non | 200 systématique | — |
| POST | `/api/auth/reset-password` | non | 200 | 400, 410 |
| GET | `/api/users/me` | oui | 200 | 401 |
| PUT | `/api/users/me` | oui | 200 | 400, 401 |

### DTOs de référence (§6 du prompt)

**Requêtes :**
- `SignUpRequestDTO(email, password, firstName, lastName)` — `@Email`, `@NotBlank`, `@Pattern(8 car min, 1 maj, 1 chiffre)`
- `SignInRequestDTO(email, password)`
- `RefreshRequestDTO(refreshToken)`
- `ForgotPasswordRequestDTO(email)`
- `ResetPasswordRequestDTO(token, newPassword)`
- `UpdateProfileRequestDTO(firstName, lastName)`

**Réponses :**
- `AuthResponseDTO(accessToken, refreshToken, expiresIn)`
- `UserProfileDTO(id, email, firstName, lastName, role, status, createdAt, lastLoginAt)` — `role` = rôle principal calculé, jamais la collection complète

---

## Proposed Changes

---

### Phase 0 — Nettoyage

Suppression de tout le code existant :
- `src/main/java/com/example/` → supprimé intégralement
- `src/test/java/com/example/` → supprimé intégralement
- `src/main/resources/db/migration/V1__create_tables.sql` → supprimé
- `src/main/resources/db/migration/V2__seed_users_and_places.sql` → supprimé
- `src/main/resources/application.properties` + `application-postgres.properties` → remplacés par `application.yml`

---

### Phase 1 — Maven & Configuration (Workflow 1)

#### [MODIFY] [pom.xml](file:///home/eulgen/Documents/Springboot%20project/findme/pom.xml)
- `groupId` → `com.geolink.findme`
- `artifactId` → `auth-service`
- Conserver toutes les dépendances existantes (Spring Boot Web, JPA, Security, JJWT, Flyway, Springdoc, Lombok, Actuator, Mail)
- **Ajouter** `testcontainers-bom` + `testcontainers-postgresql` (scope `test`)

#### [NEW] `src/main/resources/application.yml`
Datasource via variables d'env, JWT avec `securite.jwt.secret` / `duree-acces-minutes` / `duree-rafraichissement-jours`, Actuator `/actuator/health`, Flyway schéma `authservice`

#### [MODIFY] [docker-compose.yml](file:///home/eulgen/Documents/Springboot%20project/findme/docker-compose.yml)
- Service `auth-service` + `auth-db` (PostgreSQL 16)
- Variables d'env : `JWT_SECRET`, `DB_URL`, `DB_USER`, `DB_PASSWORD`
- Schéma PostgreSQL `authservice`

#### [NEW] `src/main/java/com/geolink/findme/authservice/AuthServiceApplication.java`
Point d'entrée `@SpringBootApplication`

---

### Phase 2 — Migrations Flyway (Workflow 2)

#### [NEW] `V1__init_schema.sql`
Tables dans schéma `authservice` : `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `refresh_tokens`, `password_reset_tokens`
- Contraintes `UNIQUE` sur `users.email`, `roles.name`, `permissions.code`, `refresh_tokens.token_hash`, `password_reset_tokens.token_hash`
- Clés composites sur `user_roles` et `role_permissions`

#### [NEW] `V2__seed_rbac.sql`
- 3 rôles : `USER`, `ADMIN`, `SUPPORT_AGENT`
- 6 permissions : `USER_LIST_VIEW`, `USER_ROLE_MANAGE`, `USER_STATUS_MANAGE`, `ADDRESS_LIST_VIEW_ALL`, `SUPPORT_TICKET_VIEW`, `SUPPORT_TICKET_MANAGE`
- Associations `role_permissions` conformes au tableau §3 du prompt

#### [NEW] `V3__seed_demo_data.sql` *(bonus, non bloquant)*
1 compte par rôle pour la démonstration/soutenance

---

### Phase 3 — Package `entity` (Workflow 3)

#### [NEW] `entity/AccountStatus.java`
Enum : `ACTIVE`, `INACTIVE`

#### [NEW] `entity/Permission.java`
`id`, `code`, `description`

#### [NEW] `entity/Role.java`
`id`, `name`, `description`, `permissions` (`Set<Permission>`, `@ManyToMany`, **`fetch = EAGER`** — ciblé et justifié)

#### [NEW] `entity/User.java`
`id`, `email`, `passwordHash`, `firstName`, `lastName`, `status` (`AccountStatus`), `createdAt`, `lastLoginAt`, `roles` (`Set<Role>`, `@ManyToMany`, **`fetch = EAGER`**)
Méthodes utilitaires : `isActive()`, `hasPermission(String code)`, `primaryRole()`
**Aucune** implémentation de `UserDetails`

#### [NEW] `entity/RefreshToken.java`
`id`, `tokenHash`, `user` (`@ManyToOne`), `expiration`, `revoked` — méthode `isValid()`

#### [NEW] `entity/PasswordResetToken.java`
`id`, `user`, `tokenHash`, `expiryDate`, `used` — méthodes `isExpired()`, `markUsed()`

> [!IMPORTANT]
> **Critère non-négociable** : aucune classe de `entity/` n'importe `org.springframework.security.*`

---

### Phase 4 — Package `repository` (Workflow 4)

#### [NEW] `repository/UserRepository.java`
`findByEmail(String)`, `existsByEmail(String)`

#### [NEW] `repository/RoleRepository.java`
`findByName(String)`

#### [NEW] `repository/PermissionRepository.java`
Interface standard `JpaRepository<Permission, Long>`

#### [NEW] `repository/RefreshTokenRepository.java`
`findByTokenHash(String)`, `@Modifying revokeAllByUserId(Long userId)`

#### [NEW] `repository/PasswordResetTokenRepository.java`
`findByTokenHash(String)`

---

### Phase 5 — Package `security` (Workflow 5)

#### [NEW] `security/UserPrincipal.java`
- `implements UserDetails`, **composition** (enveloppe `User`)
- `getAuthorities()` → rôles : `ROLE_<nom>` + permissions : code brut (jamais mélangé)

#### [NEW] `security/AuthUserDetailsService.java`
- `implements UserDetailsService`
- Charge `User` via `UserRepository` → retourne `UserPrincipal`

#### [NEW] `security/JwtService.java`
- `generateAccessToken(UserPrincipal)` : claims `sub` (email), `userId`, `authorities`
- `generateRefreshToken()` : UUID opaque (stocké hashé en base)
- `extractEmail(String)`, `isExpired(String)`, `extractClaims(String)`
- Clé HMAC Base64 depuis `${securite.jwt.secret}`
- **Zéro** dépendance vers `controller/` ou `dto/`

---

### Phase 6 — Package `dto` + Package `service` (Workflow 6)

#### [NEW] `dto/request/` — 6 DTOs
`SignUpRequestDTO`, `SignInRequestDTO`, `RefreshRequestDTO`, `ForgotPasswordRequestDTO`, `ResetPasswordRequestDTO`, `UpdateProfileRequestDTO`

#### [NEW] `dto/response/` — 2 DTOs
`AuthResponseDTO`, `UserProfileDTO`

#### [NEW] `dto/mapper/UserMapper.java`
Seul point de conversion `User` → `UserProfileDTO`

#### [NEW] Services (interface + impl)

| Interface | Impl | Méthodes clés |
|---|---|---|
| `AuthService` | `AuthServiceImpl` | `signUp`, `signIn`, `refresh`, `logout` |
| `UserService` | `UserServiceImpl` | `getProfile`, `updateProfile` |
| `RefreshTokenService` | `RefreshTokenServiceImpl` | `createFor(User)`, `verifyAndRotate(String)`, `revokeAllForUser(User)` |
| `PasswordResetService` | `PasswordResetServiceImpl` | `requestReset(email)`, `resetPassword(token, newPassword)` |

**Règles `@Transactional` (§5 prompt) :**
- `signUp` → `@Transactional(rollbackFor = EmailAlreadyUsedException.class)`
- `getProfile` → `@Transactional(readOnly = true)`
- Mapping vers DTO **dans** la méthode transactionnelle
- **Pas de self-invocation** `this.xxx()` entre méthodes `@Transactional` — injection croisée de beans

---

### Phase 7 — Packages `filter` + `config` (Workflow 7)

#### [NEW] `filter/JwtAuthenticationFilter.java`
`extends OncePerRequestFilter` — extraction Bearer → validation `JwtService` → chargement `UserPrincipal` → `SecurityContextHolder`
Aucune logique métier

#### [NEW] `config/SecurityConfig.java`
- `SessionCreationPolicy.STATELESS`, CSRF désactivé (API stateless sans cookie)
- `@EnableMethodSecurity`
- `JwtAuthenticationFilter` avant `UsernamePasswordAuthenticationFilter`
- `permitAll()` : `/api/auth/signup`, `/api/auth/signin`, `/api/auth/refresh`, `/api/auth/forgot-password`, `/api/auth/reset-password`, `/swagger-ui/**`, `/api-docs/**`, `/actuator/health`
- `AuthenticationEntryPoint` → 401 `ProblemDetail` JSON
- `AccessDeniedHandler` → 403 `ProblemDetail` JSON

#### [NEW] `config/BeansConfig.java`
Bean `BCryptPasswordEncoder` + bean `AuthenticationManager`

#### [NEW] `config/OpenApiConfig.java`
Schéma `bearerAuth` (HTTP, bearer, JWT), appliqué par défaut, exclu sur routes publiques

---

### Phase 8 — Packages `controller` + `exception` (Workflow 8)

#### [NEW] `controller/AuthController.java`
`/api/auth/**` — fin, délègue à `AuthService` / `PasswordResetService`, choisit le code HTTP

#### [NEW] `controller/UserController.java`
`/api/users/**` — fin, délègue à `UserService`, annotations `@PreAuthorize`

#### [NEW] Exceptions métier (5 classes)
| Exception | Code HTTP |
|---|---|
| `EmailAlreadyUsedException` | 409 |
| `InvalidCredentialsException` | 401 |
| `InvalidOrExpiredTokenException` | 400 / 410 |
| `UserNotFoundException` | 404 |

#### [NEW] `exception/GlobalExceptionHandler.java`
`@RestControllerAdvice` — mappe chaque exception vers `ProblemDetail` (RFC 7807)
Couvre aussi `MethodArgumentNotValidException` → 400

---

### Phase 9 — Documentation OpenAPI (Workflow 9)

- `@Operation` + `@ApiResponse` (401, 403, 404, 409, 410) sur chaque endpoint
- Description métier sur chaque code (pas juste le nom HTTP)
- Swagger UI accessible sans auth sur `/swagger-ui.html`

---

### Phase 10 — Tests (Workflow 10)

#### Tests unitaires (JUnit 5 + Mockito, **zéro contexte Spring**)

| Classe testée | Cas couverts |
|---|---|
| `AuthServiceImpl` | signUp nominal, email déjà utilisé (409), signIn nominal, credentials invalides (401), refresh nominal, token révoqué/expiré (401), logout |
| `UserServiceImpl` | getProfile nominal, user not found (404), updateProfile |
| `RefreshTokenServiceImpl` | createFor, verifyAndRotate (nominal, expiré, révoqué), revokeAllForUser |
| `PasswordResetServiceImpl` | requestReset nominal, compte inexistant (silencieux), resetPassword nominal, token expiré (410), token déjà utilisé (400) |

#### Tests d'intégration (Testcontainers PostgreSQL + `@SpringBootTest`)
- 8 endpoints → codes HTTP exacts
- Format `ProblemDetail` vérifié sur chaque erreur
- Token `USER` → 403 sur route nécessitant `USER_LIST_VIEW`

---

## Verification Plan

### Automated Tests
```bash
mvn clean install -DskipTests   # compile OK
mvn test                         # tous tests verts, Testcontainers gère le cycle de vie
```

### Manual Verification
```bash
docker compose up --build
curl http://localhost:8080/actuator/health   # → {"status":"UP"}
```
Puis sur Swagger UI `http://localhost:8080/swagger-ui.html` :
cycle complet `signup → signin → GET /me → refresh → logout`

---

## Checklist Workflow 11 (pré-livraison)

- [ ] 8 routes → formats conformes §4 du prompt
- [ ] Aucune entité de `entity/` dans une signature `controller/` ou `dto/`
- [ ] `entity/User` n'implémente pas `UserDetails` (c'est `UserPrincipal` qui l'enveloppe)
- [ ] Toute logique métier dans `service/`, rien dans `controller/`, `repository/`, `filter/`
- [ ] `@Transactional` correctement scopés, pas de self-invocation
- [ ] Mots de passe et tokens hashés, jamais loggés
- [ ] RBAC : `hasRole('ADMIN')` pour rôles, `hasAuthority('USER_LIST_VIEW')` pour permissions
- [ ] Messages d'erreur d'authentification génériques (pas d'énumération de comptes)
- [ ] Swagger exploitable de bout en bout
- [ ] Tests unitaires + intégration verts, cas limites couverts
- [ ] `docker compose up` démarre `auth-service` + sa base en une commande
