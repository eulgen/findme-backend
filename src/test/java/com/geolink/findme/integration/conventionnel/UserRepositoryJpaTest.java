package com.geolink.findme.integration.conventionnel;

import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.User;
import com.geolink.findme.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration JPA du repository {@link UserRepository}.
 * Vérifie le bon fonctionnement des requêtes de persistance, unicité d'email et requêtes paginées.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testuserdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Tests d'intégration JPA - UserRepository")

class UserRepositoryJpaTest {


    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        user1 = User.builder()
                .email("alice.wonder@geolink.com")
                .fullName("Alice Wonder")
                .passwordHash("hashed_password_1")
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();

        user2 = User.builder()
                .email("bob.builder@geolink.com")
                .fullName("Bob Builder")
                .passwordHash("hashed_password_2")
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();

        userRepository.save(user1);
        userRepository.save(user2);
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Deux utilisateurs enregistrés en BDD.
     * 2. When   : Recherche par email exact "alice.wonder@geolink.com".
     * 3. Then   : L'utilisateur retourné doit être présent et correspondre à Alice.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'email "bob.builder@geolink.com" existe en BDD.
     * 2. When   : Vérification de l'existence de l'email via `existsByEmail`.
     * 3. Then   : La méthode doit retourner true pour un email existant et false pour un email inconnu.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Plusieurs utilisateurs enregistrés ("Alice Wonder", "Bob Builder").
     * 2. When   : Exécution de la recherche paginée avec le mot-clé "alice" (insensible à la casse).
     * 3. Then   : La page doit contenir uniquement Alice.
     */

    @Test
    @DisplayName("Devrait trouver un utilisateur par son adresse email")
    void devrait_trouver_utilisateur_par_email() {
        Optional<User> found = userRepository.findByEmail("alice.wonder@geolink.com");

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Alice Wonder");
    }

    @Test
    @DisplayName("Devrait vérifier la présence ou l'absence d'un email en BDD")
    void devrait_verifier_existence_email() {
        boolean exists = userRepository.existsByEmail("bob.builder@geolink.com");
        boolean notExists = userRepository.existsByEmail("unknown@geolink.com");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Devrait rechercher les utilisateurs par filtre avec pagination")
    void devrait_rechercher_utilisateurs_avec_filtre_et_pagination() {
        Page<User> result = userRepository.searchUsers("alice", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("alice.wonder@geolink.com");
    }
}
