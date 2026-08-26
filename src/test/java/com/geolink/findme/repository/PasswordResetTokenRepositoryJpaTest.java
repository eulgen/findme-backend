package com.geolink.findme.repository;

import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.PasswordResetToken;
import com.geolink.findme.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration JPA du repository {@link PasswordResetTokenRepository}.
 * Vérifie la recherche par hash SHA-256 et l'invalidation des tokens de réinitialisation.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testresetdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Tests d'intégration JPA - PasswordResetTokenRepository")
class PasswordResetTokenRepositoryJpaTest {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private PasswordResetToken resetToken;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        user = User.builder()
                .email("reset.user@geolink.com")
                .fullName("Reset User")
                .passwordHash("hashed")
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();
        userRepository.save(user);

        resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash("reset_hash_123456")
                .expiryDate(Instant.now().plus(1, ChronoUnit.HOURS))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Un token de réinitialisation de mot de passe persistant.
     * 2. When   : Recherche par `findByTokenHash("reset_hash_123456")`.
     * 3. Then   : Le token doit être trouvé et `used` doit valoir false.
     */
    @Test
    @DisplayName("Devrait trouver un token de réinitialisation par son hash")
    void devrait_trouver_token_reset_par_hash() {
        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByTokenHash("reset_hash_123456");

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("reset.user@geolink.com");
        assertThat(found.get().isUsed()).isFalse();
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Un token actif pour l'utilisateur.
     * 2. When   : Exécution de `markAllUsedByUser(user)` puis `entityManager.clear()`.
     * 3. Then   : Le token relu en BDD doit passer à `used = true`.
     */
    @Test
    @DisplayName("Devrait marquer tous les tokens de réinitialisation d'un utilisateur comme utilisés")
    void devrait_marquer_tokens_utilises_par_utilisateur() {
        passwordResetTokenRepository.markAllUsedByUser(user);
        entityManager.clear(); // Vide le cache L1 pour forcer Hibernate à relire la BDD après un UPDATE bulk Modifying

        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByTokenHash("reset_hash_123456");

        assertThat(found).isPresent();
        assertThat(found.get().isUsed()).isTrue();
    }
}
