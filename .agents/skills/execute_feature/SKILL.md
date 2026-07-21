---
name: execute-feature
description: >
  Receives a validated feature prompt from list_feature, waits for user approval,
  then implements the complete Spring Boot feature following SOLID principles and
  the 20 REST best practices. Creates Entity, Repository, Service interface and impl,
  Controller, DTOs, Mapper, and exception handling with full Javadoc documentation.
  Delegates verification to verify_feature upon completion.
---

# Workflow : execute_feature

## Objectif

Implementer une fonctionnalite Spring Boot complete, proprement architecturee,
entierement documentee et respectant les standards de qualite les plus eleves :
- Principes **SOLID** (Single Responsibility, Open/Closed, Liskov, Interface Segregation, Dependency Inversion)
- **20 principes REST** pour les controllers
- **Javadoc complet** sur toutes les classes et methodes publiques
- **Gestion des erreurs** uniforme et coherente

---

## Etape 1 — Reception et analyse du prompt

Lire le prompt recu de `list_feature` (format FEATURE-[N]).

Extraire et verifier :
- Le nom de l'entite principale
- La liste des endpoints avec corps et reponses attendues
- Les champs du modele de donnees et leurs types Java
- Les relations JPA eventuelles
- Les fichiers a creer

**Si le prompt est incomplet ou ambigu** : lister les points d'ambiguite et
demander des clarifications a l'utilisateur **avant** de coder quoi que ce soit.

---

## Etape 2 — Validation utilisateur avant implementation

Presenter a l'utilisateur :

```
=== PLAN D'IMPLEMENTATION : FEATURE-[N] ===

Entite principale : [NomEntite]
Table SQL         : [nom_table]

Fichiers qui vont etre crees :
  src/main/java/com/example/findme/entity/[NomEntite].java
  src/main/java/com/example/findme/repository/[NomEntite]Repository.java
  src/main/java/com/example/findme/service/[NomEntite]Service.java
  src/main/java/com/example/findme/service/impl/[NomEntite]ServiceImpl.java
  src/main/java/com/example/findme/controller/[NomEntite]Controller.java
  src/main/java/com/example/findme/dto/request/[NomEntite]RequestDTO.java
  src/main/java/com/example/findme/dto/response/[NomEntite]ResponseDTO.java
  src/main/java/com/example/findme/mapper/[NomEntite]Mapper.java

Endpoints qui vont etre implementes :
  [liste]

Validez-vous ce plan ? (VALIDER / modifications)
```

**IMPORTANT : Ne jamais ecrire de code avant la validation explicite de l'utilisateur.**

---

## Etape 3 — Implementation

### 3.0 Elements communs (creer si absents)

Verifier l'existence de ces fichiers. Les creer en premier s'ils sont absents.

#### exception/ApiErrorResponse.java

```java
package com.example.findme.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Objet de reponse uniforme pour toutes les erreurs de l'API.
 * Respecte le format RFC 7807 (Problem Details for HTTP APIs).
 *
 * @param status    Le code de statut HTTP
 * @param message   Le message d'erreur principal
 * @param errors    La liste des erreurs de detail (validation, etc.)
 * @param timestamp L'horodatage de l'erreur
 * @param path      Le chemin de la requete qui a echoue
 */
public record ApiErrorResponse(
    int status,
    String message,
    List<String> errors,
    LocalDateTime timestamp,
    String path
) {}
```

#### exception/ResourceNotFoundException.java

```java
package com.example.findme.exception;

/**
 * Exception levee lorsqu'une ressource demandee est introuvable en base de donnees.
 * Declenchee par le GlobalExceptionHandler en reponse HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    /**
     * Construit une ResourceNotFoundException avec les informations de la ressource manquante.
     *
     * @param resourceName le nom de l'entite recherchee (ex: "User")
     * @param fieldName    le nom du champ utilise pour la recherche (ex: "id")
     * @param fieldValue   la valeur du champ utilise pour la recherche (ex: 42)
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s non trouve avec %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName()    { return fieldName; }
    public Object getFieldValue()   { return fieldValue; }
}
```

#### exception/GlobalExceptionHandler.java

```java
package com.example.findme.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions pour l'API REST.
 * Intercepte toutes les exceptions non gerees et retourne une reponse
 * JSON uniforme conforme au format ApiErrorResponse.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Gere les erreurs de ressource introuvable (404 Not Found).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Ressource introuvable : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            new ApiErrorResponse(404, ex.getMessage(), List.of(), LocalDateTime.now(), request.getRequestURI())
        );
    }

    /**
     * Gere les erreurs de validation Bean Validation (@Valid) (400 Bad Request).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());
        log.warn("Erreur de validation : {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ApiErrorResponse(400, "Erreur de validation", errors, LocalDateTime.now(), request.getRequestURI())
        );
    }

    /**
     * Gere toutes les exceptions non prevues (500 Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Erreur interne inattendue : {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ApiErrorResponse(500, "Erreur interne du serveur", List.of(ex.getMessage()),
                LocalDateTime.now(), request.getRequestURI())
        );
    }
}
```

---

### 3.1 Entity JPA

Fichier : `src/main/java/com/example/findme/entity/[NomEntite].java`

Principes SOLID appliques :
- **S** (Single Responsibility) : represente uniquement la structure de donnees persistee
- Pas de logique metier dans l'entite

Modele :
```java
package com.example.findme.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Entite JPA representant [description metier de l'entite].
 * Persistee dans la table {@code [nom_table]} de la base de donnees MySQL.
 *
 * <p>Utilise Lombok pour reduire le code boilerplate :
 * {@code @Data} genere getters/setters/equals/hashCode/toString,
 * {@code @Builder} active le pattern Builder.</p>
 *
 * @author findme-team
 * @version 1.0.0
 */
@Entity
@Table(name = "[nom_table]")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class [NomEntite] {

    /** Identifiant unique auto-genere par la base de donnees. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** [Description du champ]. Ne peut pas etre nul ni vide. */
    @Column(nullable = false, length = 255)
    @NotBlank(message = "[champ] est obligatoire")
    private String [champ];

    /** Date et heure de creation, renseignee automatiquement. */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Date et heure de derniere modification, mise a jour automatiquement. */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

---

### 3.2 Repository Spring Data JPA

Fichier : `src/main/java/com/example/findme/repository/[NomEntite]Repository.java`

Principes SOLID appliques :
- **I** (Interface Segregation) : ne definir que les methodes reellement utilisees
- **D** (Dependency Inversion) : le service depend de cette interface, pas d'une implementation concrete

Modele :
```java
package com.example.findme.repository;

import com.example.findme.entity.[NomEntite];
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entite {@link [NomEntite]}.
 *
 * <p>Etend {@link JpaRepository} pour beneficier des operations CRUD standard.
 * Des methodes de requete derivees sont ajoutees selon les besoins metier.</p>
 */
@Repository
public interface [NomEntite]Repository extends JpaRepository<[NomEntite], Long> {

    /**
     * Recherche une entite par son [champ].
     *
     * @param [champ] la valeur a rechercher (insensible a la casse recommandee)
     * @return un {@link Optional} contenant l'entite si trouvee, vide sinon
     */
    Optional<[NomEntite]> findBy[Champ](String [champ]);

    /**
     * Verifie si une entite existe avec le [champ] donne.
     *
     * @param [champ] la valeur a verifier
     * @return {@code true} si une entite avec ce [champ] existe
     */
    boolean existsBy[Champ](String [champ]);
}
```

---

### 3.3 DTOs (Data Transfer Objects)

#### RequestDTO

Fichier : `src/main/java/com/example/findme/dto/request/[NomEntite]RequestDTO.java`

```java
package com.example.findme.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO de requete pour la creation et la mise a jour de [NomEntite].
 *
 * <p>Contient uniquement les champs modifiables par le client.
 * Les champs generes automatiquement (id, createdAt, updatedAt)
 * sont exclus de ce DTO.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class [NomEntite]RequestDTO {

    /** [Description]. Obligatoire, entre 2 et 255 caracteres. */
    @NotBlank(message = "[champ] est obligatoire")
    @Size(min = 2, max = 255, message = "[champ] doit contenir entre 2 et 255 caracteres")
    private String [champ];
}
```

#### ResponseDTO

Fichier : `src/main/java/com/example/findme/dto/response/[NomEntite]ResponseDTO.java`

```java
package com.example.findme.dto.response;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO de reponse pour l'exposition de [NomEntite] via l'API REST.
 *
 * <p>Inclut les metadonnees systeme (id, createdAt, updatedAt).
 * Ne contient jamais d'informations sensibles (mots de passe, tokens, etc.).</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class [NomEntite]ResponseDTO {

    /** Identifiant unique de la ressource. */
    private Long id;

    /** [Description du champ]. */
    private String [champ];

    /** Date et heure de creation de la ressource. */
    private LocalDateTime createdAt;

    /** Date et heure de derniere modification. */
    private LocalDateTime updatedAt;
}
```

---

### 3.4 Mapper

Fichier : `src/main/java/com/example/findme/mapper/[NomEntite]Mapper.java`

Principes SOLID appliques :
- **S** (Single Responsibility) : responsabilite unique de convertir entre Entity et DTOs

```java
package com.example.findme.mapper;

import com.example.findme.dto.request.[NomEntite]RequestDTO;
import com.example.findme.dto.response.[NomEntite]ResponseDTO;
import com.example.findme.entity.[NomEntite];
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Composant de mapping entre l'entite {@link [NomEntite]} et ses DTOs.
 *
 * <p>Centralise toute la logique de conversion pour eviter la duplication
 * et faciliter la maintenance. Ce mapper est sans etat (stateless).</p>
 */
@Component
public class [NomEntite]Mapper {

    /**
     * Convertit un {@link [NomEntite]RequestDTO} en entite {@link [NomEntite]}.
     *
     * @param dto le DTO de requete source (ne doit pas etre null)
     * @return l'entite JPA correspondante
     */
    public [NomEntite] toEntity([NomEntite]RequestDTO dto) {
        return [NomEntite].builder()
            .[champ](dto.get[Champ]())
            .build();
    }

    /**
     * Convertit une entite {@link [NomEntite]} en {@link [NomEntite]ResponseDTO}.
     *
     * @param entity l'entite source (ne doit pas etre null)
     * @return le DTO de reponse correspondant
     */
    public [NomEntite]ResponseDTO toResponseDTO([NomEntite] entity) {
        return [NomEntite]ResponseDTO.builder()
            .id(entity.getId())
            .[champ](entity.get[Champ]())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    /**
     * Convertit une liste d'entites en liste de DTOs de reponse.
     *
     * @param entities la liste d'entites source
     * @return la liste de DTOs correspondante
     */
    public List<[NomEntite]ResponseDTO> toResponseDTOList(List<[NomEntite]> entities) {
        return entities.stream()
            .map(this::toResponseDTO)
            .collect(Collectors.toList());
    }
}
```

---

### 3.5 Service Interface

Fichier : `src/main/java/com/example/findme/service/[NomEntite]Service.java`

Principes SOLID appliques :
- **O** (Open/Closed) : l'interface est fermee a la modification, ouverte a l'extension
- **D** (Dependency Inversion) : le controller depend de cette abstraction

```java
package com.example.findme.service;

import com.example.findme.dto.request.[NomEntite]RequestDTO;
import com.example.findme.dto.response.[NomEntite]ResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * Interface de service definissant le contrat metier pour la gestion de {@link [NomEntite]}.
 *
 * <p>Respecte le principe de separation des interfaces (ISP) : seules les operations
 * necessaires sont declarees. L'implementation est injectee par Spring IoC.</p>
 */
public interface [NomEntite]Service {

    /**
     * Recupere toutes les entites avec pagination.
     *
     * @param pageable parametres de pagination et de tri
     * @return une page de DTOs de reponse
     */
    Page<[NomEntite]ResponseDTO> findAll(Pageable pageable);

    /**
     * Recupere toutes les entites sans pagination.
     *
     * @return la liste complete des DTOs de reponse
     */
    List<[NomEntite]ResponseDTO> findAll();

    /**
     * Recupere une entite par son identifiant unique.
     *
     * @param id l'identifiant de la ressource
     * @return le DTO de reponse correspondant
     * @throws com.example.findme.exception.ResourceNotFoundException si introuvable
     */
    [NomEntite]ResponseDTO findById(Long id);

    /**
     * Cree une nouvelle entite en base de donnees.
     *
     * @param requestDTO le DTO de requete contenant les donnees de creation
     * @return le DTO de reponse de la ressource creee (avec id et timestamps)
     */
    [NomEntite]ResponseDTO create([NomEntite]RequestDTO requestDTO);

    /**
     * Met a jour completement une entite existante.
     *
     * @param id         l'identifiant de la ressource a mettre a jour
     * @param requestDTO le DTO contenant les nouvelles donnees
     * @return le DTO de reponse mis a jour
     * @throws com.example.findme.exception.ResourceNotFoundException si introuvable
     */
    [NomEntite]ResponseDTO update(Long id, [NomEntite]RequestDTO requestDTO);

    /**
     * Supprime une entite par son identifiant.
     *
     * @param id l'identifiant de la ressource a supprimer
     * @throws com.example.findme.exception.ResourceNotFoundException si introuvable
     */
    void delete(Long id);
}
```

---

### 3.6 Service Implementation

Fichier : `src/main/java/com/example/findme/service/impl/[NomEntite]ServiceImpl.java`

Principes SOLID appliques :
- **S** : une seule responsabilite (logique metier de [NomEntite])
- **L** (Liskov) : substitut parfait de [NomEntite]Service
- **D** : injection par constructeur (jamais @Autowired sur champ)

```java
package com.example.findme.service.impl;

import com.example.findme.dto.request.[NomEntite]RequestDTO;
import com.example.findme.dto.response.[NomEntite]ResponseDTO;
import com.example.findme.entity.[NomEntite];
import com.example.findme.exception.ResourceNotFoundException;
import com.example.findme.mapper.[NomEntite]Mapper;
import com.example.findme.repository.[NomEntite]Repository;
import com.example.findme.service.[NomEntite]Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Implementation du service {@link [NomEntite]Service}.
 *
 * <p>Contient toute la logique metier relative a la gestion de {@link [NomEntite]}.
 * Utilise {@link [NomEntite]Repository} pour l'acces aux donnees et
 * {@link [NomEntite]Mapper} pour les conversions Entity <-> DTO.</p>
 *
 * <p>Toutes les methodes d'ecriture sont annotees {@code @Transactional}
 * pour garantir l'atomicite des operations en base de donnees.</p>
 */
@Service
@Transactional(readOnly = true)
public class [NomEntite]ServiceImpl implements [NomEntite]Service {

    private static final Logger log = LoggerFactory.getLogger([NomEntite]ServiceImpl.class);

    private final [NomEntite]Repository [nomEntite]Repository;
    private final [NomEntite]Mapper [nomEntite]Mapper;

    /**
     * Constructeur avec injection de dependances.
     * L'injection par constructeur est preferee a @Autowired sur champ
     * pour faciliter les tests unitaires (injection via constructeur dans les tests).
     *
     * @param [nomEntite]Repository le repository d'acces aux donnees
     * @param [nomEntite]Mapper     le mapper de conversion Entity/DTO
     */
    public [NomEntite]ServiceImpl(
            [NomEntite]Repository [nomEntite]Repository,
            [NomEntite]Mapper [nomEntite]Mapper) {
        this.[nomEntite]Repository = [nomEntite]Repository;
        this.[nomEntite]Mapper = [nomEntite]Mapper;
    }

    /** {@inheritDoc} */
    @Override
    public Page<[NomEntite]ResponseDTO> findAll(Pageable pageable) {
        log.debug("Recuperation de tous les [nomEntite] avec pagination : {}", pageable);
        return [nomEntite]Repository.findAll(pageable)
            .map([nomEntite]Mapper::toResponseDTO);
    }

    /** {@inheritDoc} */
    @Override
    public List<[NomEntite]ResponseDTO> findAll() {
        log.debug("Recuperation de tous les [nomEntite]");
        return [nomEntite]Mapper.toResponseDTOList([nomEntite]Repository.findAll());
    }

    /** {@inheritDoc} */
    @Override
    public [NomEntite]ResponseDTO findById(Long id) {
        log.debug("Recherche du [nomEntite] avec id={}", id);
        [NomEntite] entity = [nomEntite]Repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("[NomEntite]", "id", id));
        return [nomEntite]Mapper.toResponseDTO(entity);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public [NomEntite]ResponseDTO create([NomEntite]RequestDTO requestDTO) {
        log.info("Creation d'un nouveau [nomEntite] : {}", requestDTO);
        [NomEntite] entity = [nomEntite]Mapper.toEntity(requestDTO);
        [NomEntite] saved = [nomEntite]Repository.save(entity);
        log.info("[NomEntite] cree avec succes, id={}", saved.getId());
        return [nomEntite]Mapper.toResponseDTO(saved);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public [NomEntite]ResponseDTO update(Long id, [NomEntite]RequestDTO requestDTO) {
        log.info("Mise a jour du [nomEntite] id={} avec : {}", id, requestDTO);
        [NomEntite] existing = [nomEntite]Repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("[NomEntite]", "id", id));
        // Mise a jour des champs
        existing.set[Champ](requestDTO.get[Champ]());
        [NomEntite] updated = [nomEntite]Repository.save(existing);
        log.info("[NomEntite] id={} mis a jour avec succes", id);
        return [nomEntite]Mapper.toResponseDTO(updated);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Suppression du [nomEntite] id={}", id);
        [NomEntite] entity = [nomEntite]Repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("[NomEntite]", "id", id));
        [nomEntite]Repository.delete(entity);
        log.info("[NomEntite] id={} supprime avec succes", id);
    }
}
```

---

### 3.7 Controller REST

Fichier : `src/main/java/com/example/findme/controller/[NomEntite]Controller.java`

#### Checklist : 20 Principes REST — a verifier pour chaque controller

| # | Principe | Implementation |
|---|----------|----------------|
| 1 | Noms de ressources en noms pluriels | @RequestMapping("/api/v1/[noms-pluriels]") |
| 2 | Versioning dans l'URL | /api/v1/ |
| 3 | Methodes HTTP semantiques | GET=lecture, POST=creation, PUT=MAJ complete, PATCH=MAJ partielle, DELETE=suppression |
| 4 | Code 200 pour GET reussi | ResponseEntity.ok(...) |
| 5 | Code 201 + Location sur POST | ResponseEntity.created(location).body(...) |
| 6 | Code 204 sur DELETE reussi | ResponseEntity.noContent().build() |
| 7 | Code 400 sur validation echouee | Via GlobalExceptionHandler |
| 8 | Code 404 si ressource absente | Via ResourceNotFoundException -> GlobalExceptionHandler |
| 9 | Code 500 pour erreur interne | Via GlobalExceptionHandler |
| 10 | Header Location sur creation | ServletUriComponentsBuilder |
| 11 | Pagination native | Pageable dans les GET de collection |
| 12 | Filtrage via @RequestParam | Parametres optionnels de query string |
| 13 | Validation des entrees | @Valid sur @RequestBody |
| 14 | Format d'erreur uniforme | ApiErrorResponse via GlobalExceptionHandler |
| 15 | Stateless | Aucune variable d'etat dans le controller |
| 16 | Separation des responsabilites | Controller = routing + validation seulement |
| 17 | Content-type explicite | produces = MediaType.APPLICATION_JSON_VALUE |
| 18 | Documentation Swagger | @Operation, @ApiResponse, @Parameter sur chaque endpoint |
| 19 | Logging entree/sortie | Logger SLF4J sur chaque methode |
| 20 | Javadoc complet | Classe + chaque methode |

Modele de controller :
```java
package com.example.findme.controller;

import com.example.findme.dto.request.[NomEntite]RequestDTO;
import com.example.findme.dto.response.[NomEntite]ResponseDTO;
import com.example.findme.service.[NomEntite]Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;

/**
 * Controller REST pour la gestion des ressources [NomEntite].
 *
 * <p>Respecte les 20 principes REST :
 * versioning (/api/v1/), methodes HTTP semantiques, codes HTTP corrects,
 * header Location sur creation, pagination, validation, format d'erreur uniforme,
 * stateless, documentation Swagger/OpenAPI.</p>
 *
 * <p>Ce controller est responsable uniquement du routage et de la validation.
 * Toute la logique metier est deleguee a {@link [NomEntite]Service}.</p>
 *
 * @author findme-team
 * @version 1.0.0
 */
@RestController
@RequestMapping(value = "/api/v1/[noms-pluriels]", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "[NomEntite]", description = "API de gestion des [noms pluriels] (CRUD complet)")
public class [NomEntite]Controller {

    private static final Logger log = LoggerFactory.getLogger([NomEntite]Controller.class);

    private final [NomEntite]Service [nomEntite]Service;

    /**
     * Constructeur avec injection de dependances par constructeur (DIP - SOLID).
     *
     * @param [nomEntite]Service le service de gestion des [nomEntite]
     */
    public [NomEntite]Controller([NomEntite]Service [nomEntite]Service) {
        this.[nomEntite]Service = [nomEntite]Service;
    }

    /**
     * Recupere tous les [nomEntite] avec pagination.
     * Principe REST #11 : pagination native via Pageable.
     *
     * @param pageable parametres de pagination (page, size, sort)
     * @return 200 OK avec la page de [nomEntite]
     */
    @GetMapping
    @Operation(summary = "Lister tous les [nomEntite]",
               description = "Retourne la liste paginee de tous les [nomEntite]")
    @ApiResponse(responseCode = "200", description = "Liste recuperee avec succes")
    public ResponseEntity<Page<[NomEntite]ResponseDTO>> findAll(Pageable pageable) {
        log.debug("GET /api/v1/[noms-pluriels] - pageable={}", pageable);
        Page<[NomEntite]ResponseDTO> page = [nomEntite]Service.findAll(pageable);
        log.debug("Retour de {} [nomEntite] sur {} total", page.getNumberOfElements(), page.getTotalElements());
        return ResponseEntity.ok(page);
    }

    /**
     * Recupere un [nomEntite] par son identifiant.
     * Principe REST #4 : 200 OK si trouve, #8 : 404 si absent.
     *
     * @param id l'identifiant unique du [nomEntite]
     * @return 200 OK avec le [nomEntite], ou 404 Not Found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un [nomEntite] par ID")
    @ApiResponse(responseCode = "200", description = "[NomEntite] trouve")
    @ApiResponse(responseCode = "404", description = "[NomEntite] introuvable")
    public ResponseEntity<[NomEntite]ResponseDTO> findById(
            @Parameter(description = "ID du [nomEntite]", required = true) @PathVariable Long id) {
        log.debug("GET /api/v1/[noms-pluriels]/{} ", id);
        return ResponseEntity.ok([nomEntite]Service.findById(id));
    }

    /**
     * Cree un nouveau [nomEntite].
     * Principe REST #5 : 201 Created + header Location.
     * Principe REST #13 : validation @Valid sur le corps.
     *
     * @param requestDTO les donnees du [nomEntite] a creer
     * @return 201 Created avec le [nomEntite] cree et le header Location
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Creer un nouveau [nomEntite]")
    @ApiResponse(responseCode = "201", description = "[NomEntite] cree avec succes")
    @ApiResponse(responseCode = "400", description = "Donnees invalides")
    public ResponseEntity<[NomEntite]ResponseDTO> create(
            @Valid @RequestBody [NomEntite]RequestDTO requestDTO) {
        log.info("POST /api/v1/[noms-pluriels] - creation : {}", requestDTO);
        [NomEntite]ResponseDTO created = [nomEntite]Service.create(requestDTO);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(created.getId()).toUri();
        log.info("[NomEntite] cree avec succes, id={}, location={}", created.getId(), location);
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Met a jour completement un [nomEntite] existant.
     * Principe REST #3 : PUT = mise a jour complete (idempotent).
     *
     * @param id         l'identifiant du [nomEntite] a modifier
     * @param requestDTO les nouvelles donnees
     * @return 200 OK avec le [nomEntite] mis a jour, ou 404 Not Found
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Mettre a jour un [nomEntite]")
    @ApiResponse(responseCode = "200", description = "[NomEntite] mis a jour")
    @ApiResponse(responseCode = "400", description = "Donnees invalides")
    @ApiResponse(responseCode = "404", description = "[NomEntite] introuvable")
    public ResponseEntity<[NomEntite]ResponseDTO> update(
            @Parameter(description = "ID du [nomEntite]") @PathVariable Long id,
            @Valid @RequestBody [NomEntite]RequestDTO requestDTO) {
        log.info("PUT /api/v1/[noms-pluriels]/{} - mise a jour : {}", id, requestDTO);
        return ResponseEntity.ok([nomEntite]Service.update(id, requestDTO));
    }

    /**
     * Supprime un [nomEntite] par son identifiant.
     * Principe REST #6 : 204 No Content sur suppression reussie.
     * Principe REST #3 : DELETE est idempotent.
     *
     * @param id l'identifiant du [nomEntite] a supprimer
     * @return 204 No Content, ou 404 Not Found
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un [nomEntite]")
    @ApiResponse(responseCode = "204", description = "[NomEntite] supprime")
    @ApiResponse(responseCode = "404", description = "[NomEntite] introuvable")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID du [nomEntite]") @PathVariable Long id) {
        log.info("DELETE /api/v1/[noms-pluriels]/{}", id);
        [nomEntite]Service.delete(id);
        log.info("[NomEntite] id={} supprime", id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## Etape 4 — Mise a jour de application.properties

Si c'est la premiere feature ou si la DB n'est pas configuree :

```properties
# Application
spring.application.name=findme

# Base de donnees MySQL (Docker)
spring.datasource.url=jdbc:mysql://localhost:3306/findme_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=findme_user
spring.datasource.password=findme_pass
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Pagination par defaut
spring.data.web.pageable.default-page-size=20
spring.data.web.pageable.max-page-size=100

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always

# Logging
logging.level.com.example.findme=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

---

## Etape 5 — Handoff a verify_feature

Une fois tous les fichiers crees :

1. Afficher la liste complete des fichiers crees avec leurs chemins
2. Notifier : "Implementation FEATURE-[N] terminee. Lancement de la verification..."
3. Invoquer le skill `verify_feature` avec :
   - Le nom de la feature : FEATURE-[N]
   - La liste des fichiers crees
   - Les endpoints implementes avec leurs methodes HTTP et chemins
   - Les cas de test attendus (CRUD complet : create, findAll, findById, update, delete)

---

## Regles imperatives (SOLID)

| Principe | Regle concrete |
|----------|---------------|
| **S** | Chaque classe a une responsabilite unique |
| **O** | Interfaces pour l'extension, pas de modification des classes existantes |
| **L** | ServiceImpl remplace parfaitement Service sans changer le comportement |
| **I** | Ne pas creer de methodes inutiles dans les interfaces |
| **D** | Injection par constructeur avec final, jamais @Autowired sur champ |

| Regle generale | Application |
|----------------|-------------|
| Jamais de null retourne | Utiliser Optional, lancer exception ou retourner liste vide |
| @Transactional sur les ecritures | create(), update(), delete() dans ServiceImpl |
| Logger sur chaque methode publique | log.debug() entree, log.info() succes, log.error() echec |
| Javadoc obligatoire | Toutes les classes et methodes publiques |
| Packages structures | entity, repository, service, service/impl, controller, dto/request, dto/response, mapper, exception, config |
