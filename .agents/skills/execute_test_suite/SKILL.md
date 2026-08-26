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

Ce workflow exécute, vérifie et diagnostique la suite de tests du projet Spring Boot **FindMe** (Unitaires, Intégration, E2E). À la fin de l'exécution, il génère un **rapport exhaustif** de tous les tests effectués (réussis et échoués avec raisons). Pour TOUTE modification à effectuer dans le projet (code, tests, `pom.xml`, scripts SQL, configuration), il explique au préalable le problème résolu et attend la **validation explicite** de l'utilisateur avant d'appliquer tout changement.

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

## Phase 2 — Règle Obligatoire de Validation Préalable pour Toute Modification

Si l'exécution Maven échoue (compilation, dépendances, migrations Flyway, erreurs de contexte Spring, échecs d'assertions de tests) et nécessite une correction :

### ⚠️ RÈGLE OBLIGATOIRE ET ABSOLUE DE VALIDATION UTILISATEUR :
Avant d'effectuer la **MOINDRE MODIFICATION** sur **TOUT FICHIER** du projet (`pom.xml`, code source Java, scripts SQL/Flyway, fichiers de configuration `.properties`/`.yml`, classes de test, etc.) :

1. **Explication du problème** : Expliquer clairement quel souci ou erreur technique la modification résout (avec extrait des logs ou stacktraces à l'appui).
2. **Présentation de la modification** : Présenter la modification exacte proposée (diff, extrait de code ou snippet XML/SQL/Java).
3. **Demande de validation explicite** : Solliciter l'avis/la validation de l'utilisateur et **attendre sa réponse explicite** avant toute application dans le projet.

#### Exemple de demande de validation :
```markdown
> [!WARNING]
> Échec lors de l'exécution des tests.
> 
> **Souci identifié** : [Description claire du problème et de la cause racine]
> **Problème résolu** : [Ce que ce correctif vient résoudre et corriger dans le projet]
> **Action corrective proposée** :
> ```diff
> - code_ou_config_actuel
> + nouveau_code_ou_config
> ```
> 
> **Veuillez valider ou donner votre avis avant l'application de cette modification (Oui/Non).**
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
  [Description du correctif proposé dans le code source ou la classe de test]

---

## Phase 4 — Résolution et Re-test

1. En cas d'échecs (code métier, assertions, configuration, migrations SQL, pom.xml), analyser le diagnostic.
2. **Expliquer le problème résolu**, présenter la modification proposée et **attendre la validation explicite de l'utilisateur** avant de modifier tout fichier.
3. Une fois le correctif validé par l'utilisateur et appliqué, re-tester le périmètre concerné.
4. Répéter jusqu'à l'obtention du `BUILD SUCCESS` global.

