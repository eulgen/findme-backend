---
name: verify-feature
description: >
  Verifies a completed Spring Boot feature by creating and running unit tests
  for all layers (Repository with @DataJpaTest, Service with Mockito, Controller
  with @WebMvcTest). Reports results and loops back to execute_feature if issues
  are found. Updates features.md upon completion.
---

# Workflow : verify_feature

## Objectif

Garantir la qualite et le bon fonctionnement de chaque feature implementee
en creant et executant des tests unitaires sur chaque couche applicative.

---

## Etape 1 — Reception des informations

Recevoir de `execute_feature` :
- Le nom de la feature (FEATURE-[N])
- La liste des fichiers crees
- Les endpoints implementes
- Les cas de test attendus

---

## Etape 2 — Verification prealable du pom.xml

Avant d'executer les tests, verifier que `pom.xml` contient les bonnes dependances de test.

### Dependances de test correctes pour Spring Boot 4.x

```xml
<!-- Dependance de test principale - inclut JUnit 5, Mockito, AssertJ, Spring Test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Pour les tests de Repository avec H2 en memoire -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

**ATTENTION** : Les dependances suivantes N'EXISTENT PAS et doivent etre SUPPRIMEES si presentes :
- `spring-boot-starter-actuator-test`
- `spring-boot-starter-data-jpa-test`
- `spring-boot-starter-webmvc-test`

Si le pom.xml contient ces dependances invalides, les corriger avant d'executer les tests
et signaler la correction a l'utilisateur.

---

## Etape 3 — Creation des tests unitaires

### 3.1 Test du Repository

Fichier : `src/test/java/com/example/findme/repository/[NomEntite]RepositoryTest.java`

```java
package com.example.findme.repository;

import com.example.findme.entity.[NomEntite];
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests d'integration du repository {@link [NomEntite]Repository}.
 * Utilise @DataJpaTest pour charger uniquement la couche JPA avec H2 en memoire.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests du repository [NomEntite]")
class [NomEntite]RepositoryTest {

    @Autowired
    private [NomEntite]Repository [nomEntite]Repository;

    private [NomEntite] test[NomEntite];

    @BeforeEach
    void setUp() {
        [nomEntite]Repository.deleteAll();
        test[NomEntite] = [nomEntite]Repository.save(
            [NomEntite].builder().[champ]("valeur-test").build()
        );
    }

    @Test
    @DisplayName("Doit sauvegarder et retrouver un [nomEntite] par ID")
    void findById_shouldReturnEntity_whenExists() {
        Optional<[NomEntite]> found = [nomEntite]Repository.findById(test[NomEntite].getId());
        assertThat(found).isPresent();
        assertThat(found.get().get[Champ]()).isEqualTo("valeur-test");
    }

    @Test
    @DisplayName("Doit retourner vide si [nomEntite] inexistant")
    void findById_shouldReturnEmpty_whenNotExists() {
        Optional<[NomEntite]> found = [nomEntite]Repository.findById(999L);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Doit retourner tous les [nomEntite]")
    void findAll_shouldReturnAllEntities() {
        List<[NomEntite]> all = [nomEntite]Repository.findAll();
        assertThat(all).hasSize(1);
    }

    @Test
    @DisplayName("Doit supprimer un [nomEntite]")
    void delete_shouldRemoveEntity() {
        [nomEntite]Repository.deleteById(test[NomEntite].getId());
        assertThat([nomEntite]Repository.findById(test[NomEntite].getId())).isEmpty();
    }
}
```

---

### 3.2 Test du Service

Fichier : `src/test/java/com/example/findme/service/[NomEntite]ServiceTest.java`

```java
package com.example.findme.service;

import com.example.findme.dto.request.[NomEntite]RequestDTO;
import com.example.findme.dto.response.[NomEntite]ResponseDTO;
import com.example.findme.entity.[NomEntite];
import com.example.findme.exception.ResourceNotFoundException;
import com.example.findme.mapper.[NomEntite]Mapper;
import com.example.findme.repository.[NomEntite]Repository;
import com.example.findme.service.impl.[NomEntite]ServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du service {@link [NomEntite]ServiceImpl}.
 * Utilise Mockito pour mocker les dependances (repository et mapper).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du service [NomEntite]")
class [NomEntite]ServiceTest {

    @Mock
    private [NomEntite]Repository [nomEntite]Repository;

    @Mock
    private [NomEntite]Mapper [nomEntite]Mapper;

    @InjectMocks
    private [NomEntite]ServiceImpl [nomEntite]Service;

    private [NomEntite] entity;
    private [NomEntite]ResponseDTO responseDTO;
    private [NomEntite]RequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        entity = [NomEntite].builder().id(1L).[champ]("valeur").build();
        responseDTO = [NomEntite]ResponseDTO.builder().id(1L).[champ]("valeur").build();
        requestDTO = [NomEntite]RequestDTO.builder().[champ]("valeur").build();
    }

    @Test
    @DisplayName("findAll() doit retourner la liste complete")
    void findAll_shouldReturnAllEntities() {
        when([nomEntite]Repository.findAll()).thenReturn(List.of(entity));
        when([nomEntite]Mapper.toResponseDTOList(any())).thenReturn(List.of(responseDTO));
        List<[NomEntite]ResponseDTO> result = [nomEntite]Service.findAll();
        assertThat(result).hasSize(1);
        verify([nomEntite]Repository).findAll();
    }

    @Test
    @DisplayName("findById() doit retourner le DTO si l'entite existe")
    void findById_shouldReturnDTO_whenExists() {
        when([nomEntite]Repository.findById(1L)).thenReturn(Optional.of(entity));
        when([nomEntite]Mapper.toResponseDTO(entity)).thenReturn(responseDTO);
        [NomEntite]ResponseDTO result = [nomEntite]Service.findById(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById() doit lever ResourceNotFoundException si absent")
    void findById_shouldThrow_whenNotExists() {
        when([nomEntite]Repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> [nomEntite]Service.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create() doit sauvegarder et retourner le DTO")
    void create_shouldSaveAndReturn() {
        when([nomEntite]Mapper.toEntity(requestDTO)).thenReturn(entity);
        when([nomEntite]Repository.save(entity)).thenReturn(entity);
        when([nomEntite]Mapper.toResponseDTO(entity)).thenReturn(responseDTO);
        [NomEntite]ResponseDTO result = [nomEntite]Service.create(requestDTO);
        assertThat(result).isNotNull();
        verify([nomEntite]Repository).save(entity);
    }

    @Test
    @DisplayName("delete() doit supprimer l'entite existante")
    void delete_shouldRemoveEntity() {
        when([nomEntite]Repository.findById(1L)).thenReturn(Optional.of(entity));
        [nomEntite]Service.delete(1L);
        verify([nomEntite]Repository).delete(entity);
    }

    @Test
    @DisplayName("delete() doit lever ResourceNotFoundException si absent")
    void delete_shouldThrow_whenNotExists() {
        when([nomEntite]Repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> [nomEntite]Service.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

---

### 3.3 Test du Controller

Fichier : `src/test/java/com/example/findme/controller/[NomEntite]ControllerTest.java`

```java
package com.example.findme.controller;

import com.example.findme.dto.request.[NomEntite]RequestDTO;
import com.example.findme.dto.response.[NomEntite]ResponseDTO;
import com.example.findme.exception.GlobalExceptionHandler;
import com.example.findme.exception.ResourceNotFoundException;
import com.example.findme.service.[NomEntite]Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires du controller {@link [NomEntite]Controller}.
 * Utilise @WebMvcTest pour tester uniquement la couche HTTP (controller + filters).
 * Le service est mocke via @MockitoBean.
 */
@WebMvcTest([NomEntite]Controller.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("Tests du controller [NomEntite]")
class [NomEntite]ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private [NomEntite]Service [nomEntite]Service;

    @Test
    @DisplayName("GET /api/v1/[noms-pluriels] doit retourner 200 OK")
    void findAll_shouldReturn200() throws Exception {
        when([nomEntite]Service.findAll(any())).thenReturn(org.springframework.data.domain.Page.empty());
        mockMvc.perform(get("/api/v1/[noms-pluriels]"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/[noms-pluriels]/{id} doit retourner 200 si present")
    void findById_shouldReturn200_whenExists() throws Exception {
        [NomEntite]ResponseDTO dto = [NomEntite]ResponseDTO.builder().id(1L).build();
        when([nomEntite]Service.findById(1L)).thenReturn(dto);
        mockMvc.perform(get("/api/v1/[noms-pluriels]/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/[noms-pluriels]/{id} doit retourner 404 si absent")
    void findById_shouldReturn404_whenNotExists() throws Exception {
        when([nomEntite]Service.findById(99L))
            .thenThrow(new ResourceNotFoundException("[NomEntite]", "id", 99L));
        mockMvc.perform(get("/api/v1/[noms-pluriels]/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/[noms-pluriels] doit retourner 201 Created")
    void create_shouldReturn201() throws Exception {
        [NomEntite]RequestDTO request = [NomEntite]RequestDTO.builder().[champ]("valeur").build();
        [NomEntite]ResponseDTO response = [NomEntite]ResponseDTO.builder().id(1L).[champ]("valeur").build();
        when([nomEntite]Service.create(any())).thenReturn(response);
        mockMvc.perform(post("/api/v1/[noms-pluriels]")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST avec corps invalide doit retourner 400 Bad Request")
    void create_shouldReturn400_whenInvalidBody() throws Exception {
        [NomEntite]RequestDTO invalidRequest = new [NomEntite]RequestDTO(); // champs vides
        mockMvc.perform(post("/api/v1/[noms-pluriels]")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/[noms-pluriels]/{id} doit retourner 200 OK")
    void update_shouldReturn200() throws Exception {
        [NomEntite]RequestDTO request = [NomEntite]RequestDTO.builder().[champ]("nouvelle-valeur").build();
        [NomEntite]ResponseDTO response = [NomEntite]ResponseDTO.builder().id(1L).[champ]("nouvelle-valeur").build();
        when([nomEntite]Service.update(eq(1L), any())).thenReturn(response);
        mockMvc.perform(put("/api/v1/[noms-pluriels]/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/[noms-pluriels]/{id} doit retourner 204 No Content")
    void delete_shouldReturn204() throws Exception {
        doNothing().when([nomEntite]Service).delete(1L);
        mockMvc.perform(delete("/api/v1/[noms-pluriels]/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE sur ressource absente doit retourner 404")
    void delete_shouldReturn404_whenNotExists() throws Exception {
        doThrow(new ResourceNotFoundException("[NomEntite]", "id", 99L))
            .when([nomEntite]Service).delete(99L);
        mockMvc.perform(delete("/api/v1/[noms-pluriels]/99"))
            .andExpect(status().isNotFound());
    }
}
```

---

## Etape 4 — Creation du profil de test

Creer `src/test/resources/application-test.properties` si absent :

```properties
# Profil de test - utilise H2 en memoire
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
logging.level.org.springframework=WARN
```

---

## Etape 5 — Execution des tests

Executer les tests avec Maven :

```bash
mvn test -Dtest=[NomEntite]RepositoryTest,[NomEntite]ServiceTest,[NomEntite]ControllerTest -pl .
```

Ou tous les tests du projet :

```bash
mvn test
```

---

## Etape 6 — Analyse des resultats

### Si tous les tests passent (BUILD SUCCESS)

1. Mettre a jour `features.md` : statut = `Termine`
2. Ajouter dans la colonne Notes : "Tests OK : [N] tests passes"
3. Notifier `list_feature` : "FEATURE-[N] validee avec succes. Pret pour la suivante."
4. Afficher a l'utilisateur le rapport de succes

### Si des tests echouent (BUILD FAILURE)

Pour chaque test en echec, produire un rapport :

```
RAPPORT D'ECHEC - FEATURE-[N]
================================
Test echoue : [NomDuTest].[methodeDuTest]
Couche      : [Repository / Service / Controller]
Erreur      : [message d'erreur exact]
Attendu     : [valeur attendue]
Obtenu      : [valeur obtenue]

Correction proposee :
  [Description de la correction a apporter]
  Fichier concerne : [chemin/du/fichier.java]
  Ligne approximative : [N]

Faut-il appliquer cette correction ? (VALIDER / modifications)
```

Mettre `features.md` statut a `Echec` et attendre la validation de l'utilisateur
avant de renvoyer les corrections a `execute_feature`.

---

## Etape 7 — Boucle de correction

Si corrections validees par l'utilisateur :
1. Transmettre le rapport d'echec + corrections a `execute_feature`
2. Attendre la re-implementation
3. Re-executer les tests (retour a l'Etape 5)
4. Boucler jusqu'a BUILD SUCCESS

---

## Regles imperatives

- Toujours corriger le pom.xml avant d'executer les tests
- Un test par scenario (une assertion principale par test)
- Nommer les tests avec @DisplayName en francais descriptif
- Toujours utiliser AssertJ (assertThat) plutot que JUnit assertions directes
- Ne jamais marquer une feature comme Terminee sans BUILD SUCCESS
- Rapporter les echecs avec suffisamment de contexte pour corriger sans ambiguite
