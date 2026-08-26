# FindMe - API REST Spring Boot

**FindMe** est une API REST développée avec Spring Boot 4.1.0 pour la gestion des adresses GeoLink. Elle permet aux utilisateurs de s'inscrire, de se connecter, et de gérer leurs lieux favoris (restaurants, domicile, travail, etc.) avec authentification JWT.

## 📋 Table des matières

- [Fonctionnalités](#fonctionnalités)
- [Architecture du projet](#architecture-du-projet)
- [Technologies utilisées](#technologies-utilisées)
- [Structure du projet](#structure-du-projet)
- [Prérequis](#prérequis)
- [Installation et lancement](#installation-et-lancement)
- [Documentation API](#documentation-api)
- [Tests](#tests)

## ✨ Fonctionnalités

### Authentification
- **Inscription par email** : Création de compte avec email, nom d'utilisateur et mot de passe
- **Connexion email** : Authentification avec génération de token JWT
- **OAuth Google** : Connexion via compte Google
- **OAuth iCloud** : Connexion via compte Apple/iCloud
- **Réinitialisation mot de passe** : Envoi d'email de récupération

### Gestion des lieux (Places)
- **CRUD complet** : Création, lecture, modification, suppression des lieux
- **Limite par utilisateur** : Maximum 4 adresses par utilisateur
- **Géolocalisation** : Stockage des coordonnées latitude/longitude
- **Catégorisation** : Type de lieu (restaurant, domicile, travail, etc.)
- **Export PDF** : Génération de PDF pour une adresse

### Administration
- **Gestion des utilisateurs** : Liste et gestion des comptes utilisateurs
- **Gestion des adresses** : Vue d'ensemble de toutes les adresses
- **Support messages** : Réception et gestion des messages de support

### Sécurité
- **Authentification JWT** : Tokens JWT pour les requêtes authentifiées
- **Rôles utilisateur** : Rôles UTILISATEUR et ADMIN
- **Validation des données** : Bean Validation sur les DTOs
- **CORS configuré** : Partage des ressources entre origines

## 🏗️ Architecture du projet

Le projet suit une architecture en couches typique de Spring Boot :

```
┌─────────────────┐
│   Controllers   │ ← Gestion des requêtes HTTP
├─────────────────┤
│    Services     │ ← Logique métier
├─────────────────┤
│   Repositories  │ ← Accès aux données (JPA)
├─────────────────┤
│     Entities    │ ← Modèles de données
└─────────────────┘
```

### Couches de l'application

- **Controller** : Expose les endpoints REST (`AuthController`, `PlaceController`)
- **Service** : Contient la logique métier (`AuthService`, `PlaceService`)
- **Repository** : Interface JPA pour l'accès aux données (`UserRepository`, `PlaceRepository`)
- **Entity** : Entités JPA mappées aux tables PostgreSQL (`User`, `Place`)
- **DTO** : Objets de transfert de données pour les requêtes/réponses
- **Security** : Configuration Spring Security avec filtre JWT
- **Exception** : Gestion centralisée des exceptions

## 🛠️ Technologies utilisées

### Core
- **Java 26** : Langage de programmation
- **Spring Boot 4.1.0** : Framework principal
- **Spring MVC** : Controllers REST
- **Spring Data JPA** : ORM et repositories
- **Spring Security** : Sécurité et authentification
- **Spring Validation** : Validation des données
- **Spring Actuator** : Monitoring et health checks
- **Spring Mail** : Envoi d'emails

### Base de données
- **PostgreSQL 16** : Base de données relationnelle (dockerisée)
- **Hibernate** : ORM JPA

### Sécurité
- **JJWT 0.12.6** : Génération et validation des tokens JWT
- **BCrypt** : Hachage des mots de passe

### Documentation
- **Springdoc OpenAPI 2.8.9** : Documentation Swagger UI

### Utilitaires
- **Lombok** : Réduction du boilerplate
- **H2** : Base de données en mémoire pour les tests

### Tests
- **JUnit 5** : Framework de tests
- **Mockito** : Mocking pour les tests
- **MockMvc** : Tests des controllers
- **Spring Security Test** : Tests de sécurité

## 📁 Structure du projet

```
findme/
├── src/
│   ├── main/
│   │   ├── java/com/example/findme/
│   │   │   ├── config/           # Configuration (CORS, Security)
│   │   │   ├── controller/       # Controllers REST
│   │   │   ├── dto/              # DTOs (request/response)
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── entity/           # Entités JPA
│   │   │   ├── enums/            # Énumérations (Role)
│   │   │   ├── exception/        # Gestion des exceptions
│   │   │   ├── repository/       # Repositories JPA
│   │   │   ├── security/         # JWT, Security config
│   │   │   ├── service/          # Interfaces de service
│   │   │   │   └── impl/         # Implémentations
│   │   │   └── FindmeApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/example/findme/
│           ├── controller/       # Tests des controllers
│           ├── exception/        # Tests des exceptions
│           ├── repository/       # Tests des repositories
│           ├── security/         # Tests JWT
│           └── service/          # Tests des services
├── docker-compose.yml            # Configuration PostgreSQL
├── pom.xml                       # Dépendances Maven
├── mvw, mvw.cmd                  # Wrapper Maven
└── README.md
```

## 🚀 Prérequis

- **Java 26** ou supérieur
- **Maven 3.6+** (ou utiliser le wrapper inclus)
- **Docker** et **Docker Compose** (pour PostgreSQL)
- **Git** (optionnel)

## 📦 Installation et lancement

### 1. Cloner le projet

```bash
git clone <repository-url>
cd findme
```

### 2. Lancer la base de données PostgreSQL

```bash
docker-compose up -d
```

Cela démarre un conteneur PostgreSQL 16 avec les configurations suivantes :
- Base de données : `findme`
- Utilisateur : `findme_user`
- Mot de passe : `findme_pwd`
- Port : `5432`

### 3. Lancer l'application avec Maven

#### Option A : Avec le wrapper Maven (recommandé)

```bash
# Sur Linux/macOS
./mvnw spring-boot:run

# Sur Windows
mvnw.cmd spring-boot:run
```

#### Option B : Avec Maven installé

```bash
mvn spring-boot:run
```

#### Option C : Compiler et exécuter le JAR

```bash
# Compiler
./mvnw clean package

# Exécuter
java -jar target/findme-0.0.1-SNAPSHOT.jar
```

### 4. Vérifier le démarrage

L'application démarre sur le port `8080` par défaut. Vous pouvez vérifier :

- **Health check** : http://localhost:8080/actuator/health
- **Swagger UI** : http://localhost:8080/swagger-ui.html

### 5. Arrêter l'application

- **Arrêter PostgreSQL** : `docker-compose down`
- **Arrêter l'application** : `Ctrl+C` dans le terminal

## 📚 Documentation API

Une fois l'application démarrée, accédez à la documentation interactive :

**Swagger UI** : http://localhost:8080/swagger-ui.html

La documentation inclut :
- Tous les endpoints disponibles
- Les schémas de requête/réponse
- La possibilité de tester les endpoints directement

### Endpoints principaux

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/auth/signup` | Inscription d'un utilisateur |
| POST | `/api/auth/signin` | Connexion et génération JWT |
| POST | `/api/auth/google` | Connexion OAuth Google |
| POST | `/api/auth/icloud` | Connexion OAuth iCloud |
| POST | `/api/auth/password-reset` | Réinitialisation mot de passe |
| GET | `/api/places` | Liste des lieux de l'utilisateur |
| POST | `/api/places` | Créer un nouveau lieu |
| GET | `/api/places/{id}` | Récupérer un lieu par ID |
| PUT | `/api/places/{id}` | Modifier un lieu |
| DELETE | `/api/places/{id}` | Supprimer un lieu |
| GET | `/api/admin/users` | Liste des utilisateurs (admin) |
| POST | `/support/messages` | Envoyer un message support |

## 🧪 Tests

Le projet inclut des tests unitaires pour chaque couche :

### Lancer tous les tests

```bash
./mvnw test
```

### Lancer les tests d'une couche spécifique

```bash
# Tests des controllers
./mvnw test -Dtest=AuthControllerTest,PlaceControllerTest

# Tests des services
./mvnw test -Dtest=AuthServiceTest,PlaceServiceTest

# Tests des repositories
./mvnw test -Dtest=UserRepositoryTest,PlaceRepositoryTest
```

### Couverture de tests

Les tests couvrent :
- **Repository** : Tests avec `@DataJpaTest` et H2
- **Service** : Tests avec Mockito
- **Controller** : Tests avec `@WebMvcTest` et MockMvc
- **Security** : Tests JWT et Spring Security
- **Exception** : Tests du gestionnaire d'exceptions global

## 🔧 Configuration

La configuration de l'application se trouve dans `src/main/resources/application.properties` :

```properties
# Serveur
server.port=8080

# Base de données PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/findme
spring.datasource.username=findme_user
spring.datasource.password=findme_pwd
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# JWT
jwt.secret=votre-secret-key
jwt.expiration=86400000

# Documentation Swagger
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

## 📝 Notes de développement

- Le projet utilise **Lombok** pour réduire le boilerplate
- Les mots de passe sont hachés avec **BCrypt**
- Les tokens JWT expirent après 24 heures (configurable)
- La limite de 4 adresses par utilisateur est appliquée au niveau service
- Le CORS est configuré pour autoriser les requêtes cross-origin

## 🤝 Contribution

Pour contribuer au projet :
1. Fork le repository
2. Créer une branche (`git checkout -b feature/ma-feature`)
3. Commit les changements (`git commit -am 'Ajout nouvelle feature'`)
4. Push vers la branche (`git push origin feature/ma-feature`)
5. Ouvrir une Pull Request

## 📄 Licence

Ce projet est développé à des fins éducatives et commerciales.

---

**Version** : 0.0.1-SNAPSHOT  
**Date de création** : 2026-07-15  
**Auteurs** : FindMe Team
