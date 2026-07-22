package com.example.findme.repository;

import com.example.findme.entity.User;
import com.example.findme.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests d'integration du repository {@link UserRepository}.
 *
 * <p>Utilise {@code @SpringBootTest(webEnvironment = NONE)} car
 * {@code @DataJpaTest} n'est plus disponible dans Spring Boot 4.x.
 * Le profil "test" active la base H2 en memoire via
 * {@code application-test.properties}.</p>
 *
 * <p>{@code @Transactional} garantit que chaque test est execute
 * dans une transaction qui est automatiquement annulee (rollback)
 * apres l'execution, assurant l'independance entre les tests.</p>
 *
 * @author findme-team
 * @version 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("Tests du UserRepository")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    /**
     * Initialisation avant chaque test : creation d'un utilisateur de reference.
     * Chaque test repart d'un etat propre grace au rollback @Transactional.
     */
    @BeforeEach
    void setUp() {
        testUser = userRepository.save(
            User.builder()
                .email("test@findme.com")
                .username("testuser")
                .password("$2a$10$hashedpassword")
                .role(Role.UTILISATEUR)
                .phoneNumber("+237600000001")
                .build()
        );
    }

    // =========================================================
    // Tests : findById
    // =========================================================

    @Test
    @DisplayName("findById() doit retourner l'utilisateur si l'ID existe")
    void findById_shouldReturnUser_whenIdExists() {
        Optional<User> found = userRepository.findById(testUser.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@findme.com");
        assertThat(found.get().getUsername()).isEqualTo("testuser");
        assertThat(found.get().getRole()).isEqualTo(Role.UTILISATEUR);
    }

    @Test
    @DisplayName("findById() doit retourner vide si l'ID n'existe pas")
    void findById_shouldReturnEmpty_whenIdNotExists() {
        Optional<User> found = userRepository.findById(9999L);
        assertThat(found).isEmpty();
    }

    // =========================================================
    // Tests : findByEmail
    // =========================================================

    @Test
    @DisplayName("findByEmail() doit retourner l'utilisateur si l'email existe")
    void findByEmail_shouldReturnUser_whenEmailExists() {
        Optional<User> found = userRepository.findByEmail("test@findme.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(testUser.getId());
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("findByEmail() doit retourner vide si l'email n'existe pas")
    void findByEmail_shouldReturnEmpty_whenEmailNotExists() {
        Optional<User> found = userRepository.findByEmail("unknown@findme.com");
        assertThat(found).isEmpty();
    }

    // =========================================================
    // Tests : existsByEmail
    // =========================================================

    @Test
    @DisplayName("existsByEmail() doit retourner true si l'email existe deja")
    void existsByEmail_shouldReturnTrue_whenEmailExists() {
        boolean exists = userRepository.existsByEmail("test@findme.com");
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail() doit retourner false si l'email n'existe pas")
    void existsByEmail_shouldReturnFalse_whenEmailNotExists() {
        boolean exists = userRepository.existsByEmail("nouveau@findme.com");
        assertThat(exists).isFalse();
    }

    // =========================================================
    // Tests : existsByUsername
    // =========================================================

    @Test
    @DisplayName("existsByUsername() doit retourner true si le username existe deja")
    void existsByUsername_shouldReturnTrue_whenUsernameExists() {
        boolean exists = userRepository.existsByUsername("testuser");
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByUsername() doit retourner false si le username n'existe pas")
    void existsByUsername_shouldReturnFalse_whenUsernameNotExists() {
        boolean exists = userRepository.existsByUsername("newuser");
        assertThat(exists).isFalse();
    }

    // =========================================================
    // Tests : findByRole
    // =========================================================

    @Test
    @DisplayName("findByRole() doit separer les utilisateurs par role")
    void findByRole_shouldReturnUsers_withMatchingRole() {
        userRepository.save(
            User.builder()
                .email("admin@findme.com")
                .username("adminuser")
                .password("$2a$10$hashedpassword")
                .role(Role.ADMIN)
                .build()
        );

        Page<User> utilisateurs = userRepository.findByRole(Role.UTILISATEUR, PageRequest.of(0, 10));
        Page<User> admins      = userRepository.findByRole(Role.ADMIN,       PageRequest.of(0, 10));

        assertThat(utilisateurs.getTotalElements()).isEqualTo(1);
        assertThat(admins.getTotalElements()).isEqualTo(1);
        assertThat(utilisateurs.getContent().get(0).getEmail()).isEqualTo("test@findme.com");
        assertThat(admins.getContent().get(0).getEmail()).isEqualTo("admin@findme.com");
    }

    // =========================================================
    // Tests : timestamps automatiques
    // =========================================================

    @Test
    @DisplayName("createdAt et updatedAt doivent etre renseignes automatiquement")
    void save_shouldAutoPopulateTimestamps() {
        assertThat(testUser.getCreatedAt()).isNotNull();
        assertThat(testUser.getUpdatedAt()).isNotNull();
    }

    // =========================================================
    // Tests : role par defaut
    // =========================================================

    @Test
    @DisplayName("Le role par defaut d'un nouvel utilisateur doit etre UTILISATEUR")
    void save_shouldDefaultRoleToUtilisateur() {
        User userSansRole = userRepository.save(
            User.builder()
                .email("sansrole@findme.com")
                .username("sansrole")
                .password("$2a$10$hashedpassword")
                .build()
        );
        assertThat(userSansRole.getRole()).isEqualTo(Role.UTILISATEUR);
    }

    // =========================================================
    // Tests : suppression
    // =========================================================

    @Test
    @DisplayName("delete() doit supprimer l'utilisateur de la base")
    void delete_shouldRemoveUser() {
        Long id = testUser.getId();
        userRepository.delete(testUser);
        userRepository.flush();
        assertThat(userRepository.findById(id)).isEmpty();
    }
}
