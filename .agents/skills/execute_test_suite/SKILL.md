---
name: execute-test-suite
description: >
  Exécute et vérifie la suite de tests du projet Spring Boot avec option de filtrage
  (Tests Unitaires uniquement, Tests d'Intégration uniquement, Tests E2E, ou ciblage par classe/paquet/méthode).
  Analyse automatiquement les échecs et génère des diagnostics précis.
---

# Workflow : execute-test-suite

## Modèles IA Recommandés
Ce workflow utilise l'un des modèles IA suivants :
- **Gemini 3.6 Flash (High)**
- **Gemini 3.5 Flash (High)**
- **Claude Sonnet 4.6 (Thinking)**

---

## Objectif

Ce workflow exécute, vérifie et diagnostique la suite de tests du projet Spring Boot **FindMe** (Unitaires, Intégration, E2E). À la fin de l'exécution, il génère un **rapport exhaustif** de tous les tests effectués (réussis et échoués avec raisons). En cas de problème de dépendance dans le `pom.xml`, il notifie l'utilisateur et demande sa **validation explicite** avant toute résolution.

---

## Phase 1 — Choix du Périmètre d'Exécution

Sélectionner l'une des commandes Maven selon le périmètre demandé :

1. **Suite complète (Tous les tests)** :
   ```bash
   ./mvnw test
   ```
2. **Tests Unitaires uniquement** :
   ```bash
   ./mvnw test -Dtest=*Test,!*SpringBootTest,!*IntegrationTest
   ```
3. **Tests d'Intégration & E2E uniquement** :
   ```bash
   ./mvnw test -Dtest=*SpringBootTest,*IntegrationTest
   ```
4. **Ciblage par classe, paquetage ou méthode** :
   - Classe : `./mvnw test -Dtest=AuthServiceImplTest`
   - Paquetage : `./mvnw test -Dtest=com.geolink.findme.service.**.*Test`
   - Méthode : `./mvnw test -Dtest=AuthServiceImplTest#devrait_inscrire_un_nouvel_utilisateur_et_envoyer_otp`

---

## Phase 2 — Détection Spécifique des Problèmes de Dépendances (`pom.xml`)

Si l'exécution Maven échoue pendant la phase de compilation de test ou de chargement des dépendances, vérifier immédiatement les logs pour repérer les erreurs de type :
- `ClassNotFoundException` / `NoClassDefFoundError` (ex: dépendance de test manquante dans `pom.xml`).
- `Cannot resolve symbol` lors de la compilation des tests.
- Dépendances invalides ou inexistantes dans Spring Boot 4.x (ex: `spring-boot-starter-actuator-test`, `spring-boot-starter-data-jpa-test`).
- Incompatibilité de version de dépendances.

### ⚠️ RÈGLE OBLIGATOIRE DE VALIDATION UTILISATEUR :
Si une erreur de dépendance est identifiée :
1. **Notifier l'utilisateur** avec la description exacte de la dépendance manquante ou problématique.
2. **Présenter la modification exacte** à apporter dans le `pom.xml` (diff ou snippet XML).
3. **Demander la validation explicite de l'utilisateur** avant de modifier le fichier `pom.xml`.

#### Exemple de demande de validation :
```markdown
> [!WARNING]
> Échec des tests lié à un problème de dépendance Maven (`pom.xml`).
> 
> **Cause** : La classe `org.h2.Driver` est introuvable lors de l'exécution des tests d'intégration.
> **Action corrective proposée** : Ajouter la dépendance `h2` avec scope `test` dans `pom.xml` :
> ```xml
> <dependency>
>     <groupId>com.h2database</groupId>
>     <artifactId>h2</artifactId>
>     <scope>test</scope>
> <dependency>
> ```
> 
> **Veuillez valider l'application de cette modification dans `pom.xml` (Oui/Non).**
```

---

## Phase 3 — Rapport Exhaustif de Fin d'Exécution

À la fin de l'exécution des tests, quel que soit le résultat (succès ou échec), générer et présenter à l'utilisateur un **rapport récapitulatif complet** au format suivant :

```markdown
# 📊 Rapport Exhaustif d'Exécution des Tests

**Périmètre exécuté** : `[Totalité / Unitaires seuls / Intégration seuls / Ciblé]`
**Date et heure** : `[Date]`
**Résultat global** : `[BUILD SUCCESS / BUILD FAILURE]`

---

### 📈 Synthèse Numérique
| Statut | Nombre | Pourcentage |
| :--- | :---: | :---: |
| 🟢 Tests réussis | [X] | [X%] |
| 🔴 Tests échoués | [Y] | [Y%] |
| ⚪ Tests ignorés/skippés | [Z] | [Z%] |
| 📋 **Total exécutés** | **[N]** | **100%** |

---

### 🟢 Liste des Tests Réussis
- `com.geolink.findme.service.AuthServiceImplTest`
  - `devrait_inscrire_un_nouvel_utilisateur_et_envoyer_otp()`
  - `devrait_connecter_un_utilisateur_verifie_et_retourner_les_tokens()`
  - `devrait_lever_une_exception_si_l_email_est_deja_utilise()`
- `com.geolink.findme.service.UserServiceImplTest`
  - `devrait_retourner_le_profil_lorsque_l_utilisateur_existe()`
  - `devrait_mettre_a_jour_le_profil_utilisateur_avec_succes()`

---

### 🔴 Liste des Tests Échoués (avec Raisons et Diagnostics)

#### 1. `[NomClasseTest].[nomMethodeTest]`
- **Couche** : `[Service / Controller / Repository / Integration]`
- **Cause de l'échec** : `[Raison synthétique, ex: AssertionError - expected 'Pierre' but was 'Pierre Martin']`
- **Extrait du log d'erreur** :
  ```
  [Stacktrace ou message d'erreur d'assertion exact]
  ```
- **Raison détaillée / Diagnostic** :
  [Explication claire de la cause technique de l'échec]
- **Action corrective recommandée** :
  [Description du correctif dans le code source ou la classe de test]

---

## Phase 4 — Résolution et Re-test

1. En cas d'échecs dus au code métier ou aux assertions de test, proposer la solution à l'utilisateur.
2. Une fois le correctif validé et appliqué, re-tester automatiquement le périmètre concerné.
3. Répéter jusqu'à l'obtention du `BUILD SUCCESS` global.
