package com.geolink.findme.repository;

import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.RefreshToken;
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
 * Tests d'intégration JPA du repository {@link RefreshTokenRepository}.
 * Vérifie le stockage, la recherche par hash SHA-256 et la révocation des tokens.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testtokendb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Tests d'intégration JPA - RefreshTokenRepository")
class RefreshTokenRepositoryJpaTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private RefreshToken token1;
    private RefreshToken token2;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        user = User.builder()
                .email("token.user@geolink.com")
                .fullName("Token User")
                .passwordHash("hashed")
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();
        userRepository.save(user);

        token1 = RefreshToken.builder()
                .user(user)
                .tokenHash("hash_token_1_sha256")
                .expiration(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        token2 = RefreshToken.builder()
                .user(user)
                .tokenHash("hash_token_2_sha256")
                .expiration(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Un refresh token persistant avec le hash "hash_token_1_sha256".
     * 2. When   : Recherche par `findByTokenHash`.
     * 3. Then   : Le token doit être trouvé et non révoqué.
     */
    @Test
    @DisplayName("Devrait trouver un refresh token par son hash SHA-256")
    void devrait_trouver_refresh_token_par_hash() {
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("hash_token_1_sha256");

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("token.user@geolink.com");
        assertThat(found.get().isRevoked()).isFalse();
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Deux tokens actifs pour l'utilisateur.
     * 2. When   : Exécution de `revokeAllByUser(user)` puis vidage du cache de persistance L1 (`entityManager.clear()`).
     * 3. Then   : Les deux tokens relus en BDD doivent passer au statut `revoked = true`.
     */
    @Test
    @DisplayName("Devrait révoquer tous les refresh tokens actifs d'un utilisateur")
    void devrait_revoquer_tous_les_tokens_d_un_utilisateur() {
        refreshTokenRepository.revokeAllByUser(user);
        entityManager.clear(); // Vide le cache L1 pour forcer Hibernate à relire la BDD après un UPDATE bulk Modifying

        Optional<RefreshToken> t1 = refreshTokenRepository.findByTokenHash("hash_token_1_sha256");
        Optional<RefreshToken> t2 = refreshTokenRepository.findByTokenHash("hash_token_2_sha256");

        assertThat(t1).isPresent();
        assertThat(t1.get().isRevoked()).isTrue();
        assertThat(t2).isPresent();
        assertThat(t2.get().isRevoked()).isTrue();
    }
}
