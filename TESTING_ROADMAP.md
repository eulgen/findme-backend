# 🧪 Roadmap des Tests Unitaires - FindMe Auth-Service

Ce document recense la stratégie de tests unitaires et d'intégration complémentaires pour le microservice `auth-service`.

---

## 📊 État actuel de la couverture

| Couche / Composant | Statut | Fichiers de tests existants |
| :--- | :---: | :--- |
| **Services Métier** | ✅ **100% Couvert (18/18 tests)** | `AuthServiceImplTest`, `UserServiceImplTest`, `RefreshTokenServiceImplTest`, `PasswordResetServiceImplTest` |
| **Contrôleurs REST** | ⏳ À faire | - |
| **Composants Sécurité** | ⏳ À faire | - |
| **Filtres de Sécurité** | ⏳ À faire | - |
| **Gestionnaire d'Exceptions** | ⏳ À faire | - |
| **Mappers & DTOs** | ⏳ À faire | - |

---

## 🎯 Prochains Tests Unitaires à Implémenter

### 1. Composants de Sécurité (`com.geolink.findme.authservice.security`)

#### A. `JwtServiceTest`
* `devrait_generer_un_access_token_valide()`
* `devrait_extraire_le_username_depuis_un_token_jwt_valide()`
* `devrait_valider_un_token_jwt_valide()`
* `devrait_rejeter_un_token_jwt_expire_ou_invalide()`
* `devrait_generer_un_token_opaque_et_son_hash_sha256()`

#### B. `AuthUserDetailsServiceTest`
* `devrait_charger_un_user_principal_quand_email_existe()`
* `devrait_lever_username_not_found_exception_quand_email_n_existe_pas()`

---

### 2. Contrôleurs HTTP REST (`com.geolink.findme.authservice.controller`)
*Technologie : `@WebMvcTest` + Mockito + MockMvc.*

#### A. `AuthControllerTest` (`/api/v1/auth`)
* **`POST /signup`** :
  * `devrait_retourner_201_created_lors_de_l_inscription_valide()`
  * `devrait_retourner_400_bad_request_si_validation_dto_echoue()`
  * `devrait_retourner_409_conflict_si_email_deja_utilise()`
* **`POST /signin`** :
  * `devrait_retourner_200_ok_et_les_tokens_si_credentials_valides()`
  * `devrait_retourner_401_unauthorized_si_mauvais_mot_de_passe()`
* **`POST /refresh`** :
  * `devrait_retourner_200_ok_avec_nouveaux_tokens()`
  * `devrait_retourner_401_unauthorized_si_token_expire_ou_revoque()`
* **`POST /logout`** :
  * `devrait_retourner_200_ok_et_revoquer_les_tokens()`
* **`POST /password-reset/request` & `/confirm`** :
  * Scénarios nominaux (200 OK) et erreurs de validation (400 Bad Request).

#### B. `UserControllerTest` (`/api/v1/users`)
* **`GET /me`** :
  * `devrait_retourner_200_ok_et_le_profil_utilisateur_authentifie()`
  * `devrait_retourner_401_unauthorized_si_aucun_token_fourni()`
* **`PUT /me`** :
  * `devrait_retourner_200_ok_et_le_profil_mis_a_jour()`

---

### 3. Filtre de Sécurité (`com.geolink.findme.authservice.filter`)

#### `JwtAuthenticationFilterTest`
* `devrait_authentifier_la_requete_quand_header_bearer_jwt_est_valide()`
* `devrait_ignorer_l_authentification_si_header_authorization_absent()`
* `devrait_ne_pas_authentifier_si_token_jwt_invalide()`

---

### 4. Gestion Globale des Exceptions & Mappers

#### A. `GlobalExceptionHandlerTest`
* `devrait_retourner_404_not_found_pour_user_not_found_exception()`
* `devrait_retourner_409_conflict_pour_email_already_used_exception()`
* `devrait_retourner_401_unauthorized_pour_invalid_credentials_exception()`
* `devrait_retourner_400_bad_request_pour_erreurs_de_validation_field()`

#### B. `UserMapperTest`
* `devrait_mapper_un_user_vers_un_user_profile_dto()`

---

## 🚀 Ordre d'Exécution Recommandé

1. 🥇 **`JwtServiceTest`** *(Priorité haute - logique crypto & JWT)*
2. 🥈 **`AuthControllerTest`** *(Priorité haute - validation des endpoints REST)*
3. 🥉 **`GlobalExceptionHandlerTest`** *(Priorité moyenne - format des réponses d'erreur)*
4. 🏅 **`UserControllerTest` & `JwtAuthenticationFilterTest`** *(Priorité moyenne)*

---

*Fichier généré pour le suivi de la qualité du projet FindMe Auth Service.*
