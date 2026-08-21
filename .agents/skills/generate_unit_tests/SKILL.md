---
name: generate-unit-tests
description: >
  Analyse le projet Spring Boot pour identifier les classes manquant de tests unitaires
  (Services, Controllers, Repositories, Mappers, Filters) et génère les classes de tests
  unitaires en respectant strictement le style et les règles de nommage existants dans le projet.
  Fonctionne étape par étape avec plan préalable, explications explicites et commentaires de logique.
---

# Workflow : generate-unit-tests

## Modèles IA Recommandés
Ce workflow utilise l'un des modèles IA suivants :
- **Gemini 3.6 Flash (High)**
- **Gemini 3.5 Flash (High)**
- **Claude Sonnet 4.6 (Thinking)**

---

## Objectif

Ce workflow analyse le projet Spring Boot **FindMe**, identifie les composants applicatifs (Services, Controllers, Repositories, Mappers, Sécurité/Filtres) manquant de couverture de tests unitaires, et génère des classes de tests unitaires complets de manière **progressive et étape par étape**.

---

## RÈGLES D'OR DU WORKFLOW

1. **Plan d'implémentation préalable obligatoire** : Avant d'écrire le moindre test unitaire, établir et présenter à l'utilisateur un plan d'implémentation détaillé listant les classes cibles, les méthodes à tester et les cas de tests prévus.
2. **Exécution étape par étape avec synthèse** : À chaque étape de génération de test :
   - Indiquer ce qui a été réalisé à l'étape précédente.
   - Détailler la logique appliquée pour chaque test dans la synthèse de l'étape.
   - Annoncer ce qui sera réalisé à l'étape suivante.
3. **Commentaires de logique systématiques** : Ajouter dans chaque méthode de test des commentaires Javadoc / Java détaillant la logique testée, les hypothèses (Given), les actions (When) et les assertions attendues (Then).
4. **Style et conventions FindMe** :
   - **JUnit 5**, **Mockito** (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, `@Spy`), **AssertJ** (`assertThat`, `assertThatThrownBy`).
   - Noms de méthodes en **français** au format snake_case débutant par `devrait_...` (ex: `devrait_creer_une_adresse_lorsque_donnees_valides`).
   - Injection de propriétés via `ReflectionTestUtils.setField(...)`.

---

## Déroulement du Workflow

### Étape 0 — Analyse et Plan d'Implémentation des Tests

1. **Analyse du Codebase** : Scanner `src/main/java/com/geolink/findme/` et `src/test/java/com/geolink/findme/`.
2. **Rédaction du Plan** : Présenter le plan sous la forme suivante :

```markdown
### 📋 Plan d'Implémentation des Tests Unitaires

**Composant Cible** : [NomDuComposant] (`chemin/du/fichier.java`)

| # | Méthode à tester | Scénario de Test | Type (Nominal / Erreur / Limite) | Nom de la méthode de test |
|---|------------------|------------------|----------------------------------|---------------------------|
| 1 | `creerAdresse`   | Données valides   | Nominal                          | `devrait_creer_adresse_si_donnees_valides` |
| 2 | `creerAdresse`   | Limite de 4 adresses atteinte | Erreur Metier | `devrait_lever_exception_si_limite_atteinte` |

**Logique appliquée** : [Explication synthétique du plan de test]
```

3. Attendre la validation ou démarrer l'Étape 1 dès accord.

---

### Étape N — Génération d'un sous-ensemble / classe de tests

À CHAQUE ÉTAPE DE GÉNÉRATION, AFFICHER LE FORMAT DE SYNTHÈSE SUIVANT :

```markdown
---
### 🔄 Synthèse d'Étape [N]

#### ⬅️ Étape Précédente :
- [Description des tests créés/exécutés à l'étape N-1, ou "Initialisation du plan" si Étape 1]

#### 🎯 Étape Actuelle :
- **Classe ciblée** : `[NomClasseTest.java]`
- **Logique appliquée pour chaque test** :
  1. `[nom_methode_1]` : [Explication de la logique métier testée, des mocks configurés et des vérifications réalisées]
  2. `[nom_methode_2]` : [Explication du scénario d'exception ou de limite]

#### ➡️ Étape Suivante :
- [Description de la classe/méthode qui sera traitée à l'étape N+1]
---
```

---

## Modèles de Code avec Commentaires de Logique

### Exemple : Test de Service (`service/*Test.java`)

```java
package com.geolink.findme.service;

import com.geolink.findme.dto.mapper.AddressMapper;
import com.geolink.findme.dto.request.AddressRequestDTO;
import com.geolink.findme.dto.response.AddressResponseDTO;
import com.geolink.findme.entity.Address;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.AddressLimitExceededException;
import com.geolink.findme.exception.ResourceNotFoundException;
import com.geolink.findme.repository.AddressRepository;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.service.addressService.AddressServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du service {@link AddressServiceImpl}.
 * Vérifie le comportement des règles métiers de gestion des adresses (CRUD + limite de 4).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires du service Address")
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private AddressMapper addressMapper = new AddressMapper();

    @InjectMocks
    private AddressServiceImpl addressService;

    private User testUser;
    private Address testAddress;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("user@geolink.com").build();
        testAddress = Address.builder().id(10L).city("Paris").country("France").user(testUser).build();
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'utilisateur existe en BDD et possède actuellement 2 adresses (< 4).
     * 2. When   : On tente de créer une 3ème adresse.
     * 3. Then   : L'adresse doit être sauvegardée en BDD et le DTO retourné doit contenir les bonnes données.
     */
    @Test
    @DisplayName("Devrait créer une adresse avec succès si la limite de 4 n'est pas atteinte")
    void devrait_creer_adresse_lorsque_donnees_valides_et_limite_non_atteinte() {
        // Given - Simulation de l'existence de l'utilisateur et comptage des adresses (2 adresses actuelles)
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(addressRepository.countByUserId(1L)).thenReturn(2L);
        when(addressRepository.save(any(Address.class))).thenReturn(testAddress);

        AddressRequestDTO request = AddressRequestDTO.builder().city("Paris").country("France").build();

        // When - Appel de la méthode de création d'adresse
        AddressResponseDTO result = addressService.createAddress(1L, request);

        // Then - Vérification du résultat DTO et de l'appel effectif de sauvegarde en BDD
        assertThat(result).isNotNull();
        assertThat(result.getCity()).isEqualTo("Paris");
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'utilisateur existe mais possède déjà 4 adresses en BDD.
     * 2. When   : On tente d'ajouter une 5ème adresse.
     * 3. Then   : La méthode doit lever une exception AddressLimitExceededException sans sauvegarder en BDD.
     */
    @Test
    @DisplayName("Devrait lever une exception si l'utilisateur possède déjà 4 adresses")
    void devrait_lever_exception_si_limite_maximale_d_adresses_atteinte() {
        // Given - Simulation de 4 adresses existantes pour l'utilisateur
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(addressRepository.countByUserId(1L)).thenReturn(4L);

        AddressRequestDTO request = AddressRequestDTO.builder().city("Lyon").country("France").build();

        // When & Then - Vérification de la levée d'exception métier et absence de sauvegarde
        assertThatThrownBy(() -> addressService.createAddress(1L, request))
                .isInstanceOf(AddressLimitExceededException.class)
                .hasMessageContaining("Limite maximale de 4 adresses atteinte");

        verify(addressRepository, never()).save(any(Address.class));
    }
}
```

---

## Phase 5 — Exécution et Validation Progressive

Après l'écriture des tests d'une étape :
1. Lancer l'exécution de la classe créée :
   ```bash
   ./mvnw test -Dtest=NomClasseTest
   ```
2. Présenter la synthèse d'étape avec le résultat (`BUILD SUCCESS`) avant de passer à l'Étape suivante.
