---
name: list-feature
description: >
  Analyzes the Postman collection and user requirements to generate a
  comprehensive, prioritized feature list for the Spring Boot findme project.
  Validates the list with the user, then orchestrates execution one feature
  at a time via execute_feature. Keeps features.md updated at all times.
---

# Workflow : list_feature

## Objectif

Ce workflow est le **chef d'orchestre** du projet. Il :
1. Analyse la collection Postman et les besoins du projet
2. Genere une liste structuree de fonctionnalites
3. Obtient la validation de l'utilisateur
4. Envoie les fonctionnalites une a une a `execute_feature`
5. Met a jour `features.md` en continu

---

## Etape 1 — Verification des pre-requis

Avant tout, verifier que les fichiers suivants sont presents dans le projet :

| Fichier | Requis | Action si absent |
|---------|--------|-----------------|
| `*.postman_collection.json` | OUI | Demander a l'utilisateur de le fournir |
| `docker-compose.yml` | OUI | Demander a l'utilisateur ou proposer d'en creer un |
| `pom.xml` | OUI (present) | Verifier et corriger si besoin |
| `application.properties` | OUI (present) | Completer si besoin |

**Ne pas continuer sans la collection Postman.**

---

## Etape 2 — Analyse de la collection Postman

Lire le fichier `*.postman_collection.json` et extraire pour chaque requete :
- La **methode HTTP** (GET, POST, PUT, PATCH, DELETE)
- Le **chemin** de l'endpoint (ex: /api/v1/users/{id})
- Le **corps JSON** (si present) — les champs = attributs de l'entite
- Les **parametres de chemin** et de **query string**
- Les **headers** specifiques

### Regroupement
Regrouper par dossier Postman → chaque dossier = un **domaine metier** (entite).

### Deduction du modele de donnees
Pour chaque entite identifiee :
- Lister tous les champs avec leur **type Java probable**
- Identifier les **contraintes** (@NotNull, @Size, @Email, etc.)
- Detecter les **relations** (OneToMany, ManyToOne, ManyToMany)
- Nommer la **table SQL** correspondante (snake_case, pluriel)

### Signalement des incoherences
Avant de continuer, signaler a l'utilisateur :
- Endpoints sans corps de requete defini
- Relations de donnees non claires
- Nommages inconsistants
- Endpoints en double ou contradictoires

---

## Etape 3 — Generation de la liste de fonctionnalites

### Format de chaque feature

```
FEATURE-[N] | [Categorie] | [Titre court] | Priorite: [HAUTE/MOYENNE/BASSE]
Description : [Ce que fait cette fonctionnalite]
Endpoints   : [liste des endpoints couverts]
Entites     : [Entity1, Entity2]
Dependances : [FEATURE-X si applicable]
```

### Ordre de priorite standard

1. **Infrastructure** — Correction pom.xml, docker-compose.yml, application.properties
2. **Entites fondamentales** — Modeles JPA sans dependances externes
3. **CRUD de base** — Par entite : GET all, GET by id, POST, PUT, DELETE
4. **Gestion des erreurs** — GlobalExceptionHandler, ApiError, exceptions custom
5. **Relations** — Entites liees, jointures, sous-ressources
6. **Fonctions metier** — Recherche, filtrage, pagination avancee
7. **Securite** — JWT / Spring Security (si applicable)
8. **Documentation** — Swagger/OpenAPI

---

## Etape 4 — Presentation et validation utilisateur

Afficher le tableau recapitulatif suivant :

```
| # | Fonctionnalite          | Endpoints couverts               | Entites | Priorite | Statut     |
|---|-------------------------|----------------------------------|---------|----------|------------|
| 1 | Fix pom.xml + Docker    | N/A                              | N/A     | HAUTE    | En attente |
| 2 | Entite X + CRUD complet | GET/POST/PUT/DELETE /api/v1/[x]  | X       | HAUTE    | En attente |
| … | …                       | …                                | …       | …        | …          |
```

Puis demander :

> ✅ Voici la liste des fonctionnalites identifiees a partir de votre collection Postman.
> Voulez-vous :
> - **Valider** cette liste telle quelle ?
> - **Ajouter** des fonctionnalites ?
> - **Modifier** ou supprimer certaines entrees ?
>
> Repondez avec vos modifications ou tapez **VALIDER** pour demarrer l'execution.

**IMPORTANT : Ne pas passer a l'Etape 5 sans validation explicite de l'utilisateur.**

---

## Etape 5 — Mise a jour de features.md

Apres validation, ecrire/mettre a jour `features.md` a la racine du projet :

```markdown
# Features du projet FindMe

Cree le : [DATE]
Derniere mise a jour : [DATE]

## Legende
- En attente : fonctionnalite identifiee, pas encore commencee
- En cours   : en cours d'implementation
- Termine    : implementee et testee avec succes
- Echec      : tests en echec, correction requise

## Liste des fonctionnalites

| # | Fonctionnalite | Endpoints | Entites | Priorite | Statut | Notes |
|---|----------------|-----------|---------|----------|--------|-------|
...
```

---

## Etape 6 — Orchestration feature par feature

Pour CHAQUE fonctionnalite dans l'ordre :

**6.1** Mettre a jour `features.md` : statut = `En cours`

**6.2** Construire le prompt `execute_feature` (voir format section suivante)

**6.3** Presenter le prompt a l'utilisateur :
> === PROCHAINE FEATURE : FEATURE-[N] ===
> [prompt detaille]
> Validez-vous l'implementation de cette fonctionnalite ? (VALIDER / modifications)

**6.4** Apres validation, invoquer le skill `execute_feature`

**6.5** Attendre le resultat de `verify_feature` :
- Si **SUCCES** : mettre `features.md` a `Termine`, passer a la feature suivante
- Si **ECHEC** : voir Etape 7

---

## Etape 7 — Gestion des echecs

Si `verify_feature` signale un echec :
1. Afficher le rapport d'echec a l'utilisateur
2. Mettre `features.md` statut a `Echec`
3. Proposer la correction et attendre validation de l'utilisateur
4. Apres validation, reinvoquer `execute_feature` avec le correctif
5. Reinvoquer `verify_feature`
6. Boucler jusqu'a validation complete
7. Une fois corrige : mettre `features.md` a `Termine`

---

## Format du prompt pour execute_feature

```
=== FEATURE-[N] : [Titre] ===

CONTEXTE :
- Package de base : com.example.findme
- Spring Boot     : 4.1.0 | Java 26
- Base de donnees : MySQL (dockerise, port 3306)
- Dependances     : JPA, Lombok, MySQL Connector, Actuator

DESCRIPTION :
[Description detaillee de ce qu'il faut implementer]

ENDPOINTS A IMPLEMENTER :
- [METHODE] [chemin]
  Corps : { "champ1": "type", "champ2": "type" }
  Reponse 201/200 : { "id": 1, "champ1": "...", "createdAt": "..." }
  Codes HTTP : 201 Created | 400 Bad Request | 404 Not Found | 500 Server Error

MODELE DE DONNEES :
- Nom entite : [NomEntite]
- Table SQL   : [nom_table]
- Champs :
  * id        : Long   (@Id, @GeneratedValue automatique)
  * [champ]   : [Type] (@Column, @NotNull, contraintes)
  * createdAt : LocalDateTime (@CreationTimestamp)
  * updatedAt : LocalDateTime (@UpdateTimestamp)

RELATIONS JPA :
[si applicable : @OneToMany(mappedBy="..."), @ManyToOne, @JoinColumn(name="...")]

FICHIERS A CREER :
- entity/[NomEntite].java
- repository/[NomEntite]Repository.java
- service/[NomEntite]Service.java          (interface)
- service/impl/[NomEntite]ServiceImpl.java
- controller/[NomEntite]Controller.java
- dto/request/[NomEntite]RequestDTO.java
- dto/response/[NomEntite]ResponseDTO.java
- mapper/[NomEntite]Mapper.java
[+ exception/ResourceNotFoundException.java   si premiere entite]
[+ exception/ApiErrorResponse.java            si premiere entite]
[+ exception/GlobalExceptionHandler.java      si premiere entite]
[+ config/OpenApiConfig.java                  si feature documentation]
```

---

## Regles imperatives

- Toujours lire l'etat actuel de `features.md` avant d'agir
- Ne jamais executer deux features en parallele
- En cas de doute sur une feature, demander a l'utilisateur
- Documenter chaque decision dans `features.md` (colonne Notes)
- Ne jamais modifier du code existant sans signaler l'impact

---

## Chaine d'appel

```
list_feature ──► [validation] ──► execute_feature ──► verify_feature
                                        ▲                    │
                                        └──── (si ECHEC) ────┘
```
