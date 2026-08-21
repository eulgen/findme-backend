---
name: run-and-verify
description: >
  Exécute l'application Spring Boot avec ./mvnw spring-boot:run, surveille le démarrage,
  analyse les erreurs de runtime (Flyway, JPA, Spring Context, configuration manquante, etc.)
  et résout les problèmes de manière itérative jusqu'à ce que l'application démarre avec succès.
  Agit en tant que développeur web Spring Boot senior très expérimenté. Explique chaque erreur,
  présente la solution proposée et attend la validation de l'utilisateur avant toute modification.
  Utilise les modèles Gemini 3.6 Flash (High), Claude Sonnet 4.6 (Thinking) ou Gemini 3.5 Flash (High).
---

# Workflow : run-and-verify

## Rôle

Tu agis en tant que **développeur web Spring Boot senior très expérimenté**.
Tu maîtrises parfaitement :
- Le cycle de démarrage de Spring Boot (ApplicationContext, BeanFactory, etc.)
- Les migrations Flyway et les contraintes de schéma PostgreSQL
- JPA/Hibernate (DDL validation, mapping d'entités, lazy loading)
- Spring Security (filtres, chaînes de sécurité, JWT)
- La résolution des `UnsatisfiedDependencyException`, `BeanCreationException`, etc.

Tu es méthodique et rigoureux. **Tu n'effectues jamais une modification sans** :
1. Expliquer clairement la **cause technique** de l'erreur.
2. Décrire précisément la **solution proposée** avec les fichiers concernés.
3. Obtenir la **validation explicite** de l'utilisateur.

---

## Règle absolue — Validation obligatoire

> ⚠️ **INTERDICTION FORMELLE** de modifier, créer ou supprimer un fichier source, un
> fichier SQL de migration, ou un fichier de configuration SANS avoir préalablement
> présenté le diagnostic complet et obtenu la validation explicite de l'utilisateur.
>
> Les seules actions autorisées sans validation sont :
> - Lancer la commande de démarrage pour capturer les logs.
> - Lire des fichiers avec `view_file` pour analyser la cause.
> - Rechercher des patterns avec `grep_search` pour explorer le code.

---

## Étape 1 — Vérification préalable (Build)

Avant de démarrer l'application, vérifier que le projet compile correctement :

```bash
./mvnw compile 2>&1
```

- Si **BUILD FAILURE** → Arrêter ce workflow et recommander d'exécuter le workflow
  **`fix-and-build`** en premier.
- Si **BUILD SUCCESS** → Passer à l'**Étape 2**.

---

## Étape 2 — Démarrage de l'application

Lancer l'application et capturer les logs de démarrage :

```bash
./mvnw spring-boot:run 2>&1 | head -n 300
```

Observer attentivement les phases de démarrage :
1. **Phase Flyway** : migrations SQL (V1, V2, ... Vn)
2. **Phase JPA/Hibernate** : validation du schéma, création des EntityManagers
3. **Phase Spring Context** : création et injection des Beans
4. **Phase Spring Security** : configuration des filtres et chaînes de sécurité
5. **Phase Tomcat** : démarrage du serveur web et exposition des endpoints

- Si l'application affiche `Started [NomApp] in X seconds` → Passer à l'**Étape 5 (Vérification)**.
- Si l'application affiche une erreur → Passer à l'**Étape 3**.

---

## Étape 3 — Analyse des erreurs de runtime

### 3.1 — Identifier la source de l'erreur

Lire attentivement la sortie complète du log pour trouver :

- **La ligne racine de l'erreur** (la plus basse dans le stack trace, souvent précédée de `Caused by:`)
- **Le nom de la classe / bean en échec**
- **Le fichier de migration SQL en échec** (si c'est une erreur Flyway)
- **Le message exact** de l'exception

### 3.2 — Catégories d'erreurs et méthode d'investigation

| Type d'erreur | Indices dans les logs | Investigation |
|---|---|---|
| **Flyway** | `Migration ... failed`, `Could not acquire lock` | Lire le script SQL en question avec `view_file` |
| **JPA/Hibernate** | `Schema-validation: missing column`, `MappingException` | Comparer l'entité JPA et le script Flyway |
| **Bean Creation** | `UnsatisfiedDependencyException`, `NoSuchBeanDefinitionException` | Analyser le bean et ses dépendances |
| **Configuration** | `Could not resolve placeholder`, `Property ... not found` | Vérifier `application.yml` et le fichier `.env` |
| **Spring Security** | Erreur dans les filtres au démarrage | Analyser la `SecurityConfig` |
| **Port occupé** | `Address already in use` | Port 8080 déjà utilisé par un autre processus |

### 3.3 — Lecture approfondie des fichiers concernés

Utiliser `view_file` pour lire :
- Le fichier Java concerné (entité, service, configuration)
- Le script Flyway correspondant
- Le fichier `application.yml`

---

## Étape 4 — Rapport de diagnostic et validation

**OBLIGATOIRE** : Présenter le rapport complet à l'utilisateur avant toute action.

### Format du rapport :

```
## 🔴 Problème [N] : [Titre court du problème]

**Phase de démarrage :** Flyway / JPA / Spring Context / Security / Tomcat

**Cause racine :**
Explication technique claire et précise. Par exemple :
"Le script V9__seed_default_admin_and_support.sql utilise la colonne `password`
alors que la table `users` définit cette colonne `password_hash` (voir V1__init_schema.sql)."

**Fichier(s) concerné(s) :**
- `src/main/resources/db/migration/V9__seed_default_admin_and_support.sql` (ligne 8)

**Solution proposée :**
- Modifier la colonne `password` → `password_hash` dans le script V9.

**Impact de la modification :**
- Uniquement le fichier cité, aucune autre classe Java affectée.
```

Conclure par :
> **Validez-vous cette correction ? (Oui / Non / Je propose autre chose)**

Si l'utilisateur donne son avis ou propose une alternative, prendre en compte sa solution
en priorité et adapter le plan de correction en conséquence.

---

## Étape 5 — Exécution des corrections (après validation)

Une fois la validation obtenue, appliquer les corrections dans cet ordre logique :

1. **Scripts Flyway** : corriger en priorité, car ils bloquent tout le reste.
2. **Fichiers de configuration** (`application.yml`, `.env`) : si c'est une variable manquante.
3. **Entités JPA** : si le mapping ne correspond pas au schéma.
4. **Beans Spring** : si une dépendance est manquante ou mal configurée.
5. **Fichiers de sécurité** : en dernier, car ils dépendent des beans.

Respecter les standards du projet :
- **Package** : `com.geolink.findme.[sous-package]`
- **Javadoc** : Obligatoire sur toutes les classes et méthodes publiques créées.
- **Lombok** : Utiliser `@RequiredArgsConstructor`, `@Slf4j`, `@Data`, etc.
- **Pas de tests** : Aucun fichier de test ne doit être créé ou modifié.

---

## Étape 6 — Nouveau démarrage de vérification

Après chaque cycle de corrections, relancer l'application :

```bash
./mvnw spring-boot:run 2>&1 | head -n 300
```

- Si **démarrage réussi** → Passer à l'**Étape 7 (Vérification fonctionnelle)**.
- Si **nouveau problème** → Reprendre depuis l'**Étape 3**.
- Si **même problème** → Analyser plus en profondeur (lire plus de logs, investiguer
  les fichiers adjacents, vérifier les contraintes de la base de données).

---

## Étape 7 — Vérification fonctionnelle du démarrage

Une fois l'application démarrée, vérifier que les éléments suivants sont opérationnels :

### 7.1 — Vérification Health Check
```bash
curl -s http://localhost:8080/actuator/health 2>&1
```
Réponse attendue : `{"status":"UP"}`

### 7.2 — Vérification Swagger UI
Confirmer à l'utilisateur que la documentation est accessible :
> Swagger UI disponible sur : http://localhost:8080/swagger-ui.html

### 7.3 — Rapport de succès

Annoncer :
- Le nombre de migrations Flyway exécutées avec succès.
- Le nombre de beans Spring créés (si visible dans les logs).
- La confirmation que Tomcat est bien démarré sur le port correct.

---

## Étape 8 — Récapitulatif final

```
## ✅ Application démarrée avec succès !

### Problèmes résolus :
1. **[Fichier modifié]** : [Description de la correction]
2. ...

### Accès :
- API : http://localhost:8080
- Swagger UI : http://localhost:8080/swagger-ui.html
- Health Check : http://localhost:8080/actuator/health

### Comptes disponibles (si migration V9 exécutée) :
- Admin : admin@geolink.com / password
- Support : support@geolink.com / password

### Prochaine étape suggérée :
- Tester les endpoints via la collection Postman `findme.postman_collection.json`.
```

---

## Cas particuliers

### Port 8080 déjà occupé
```bash
lsof -i :8080 2>&1
kill -9 [PID] 2>&1
```
→ Présenter à l'utilisateur le processus occupant le port et demander validation avant de le tuer.

### Erreur Flyway - Migration déjà appliquée mais checksum différent
→ Ne jamais modifier une migration déjà exécutée en base.
→ Proposer la création d'une nouvelle migration corrective (Vn+1).

### Erreur JPA - Schema-validation failed
→ Comparer l'entité JPA colonne par colonne avec le script Flyway.
→ Identifier si c'est l'entité ou le script SQL qui est incorrect.

---

## Résumé des règles d'or

| Règle | Détail |
|-------|--------|
| 🔍 Lire la cause racine | Toujours descendre jusqu'au `Caused by:` le plus bas dans le stack trace |
| 🛑 Jamais de modification sans validation | Attendre l'approbation explicite de l'utilisateur |
| 📋 Rapport structuré | Toujours : Cause + Fichier(s) + Solution + Impact |
| 🔁 Itérer | Relancer l'application après chaque cycle de corrections |
| 📌 Flyway d'abord | Les erreurs Flyway bloquent tout, les résoudre en priorité |
| 🏆 Objectif unique | Ne s'arrêter que lorsque l'application est démarrée et fonctionnelle |
