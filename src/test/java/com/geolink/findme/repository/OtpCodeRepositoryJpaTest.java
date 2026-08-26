package com.geolink.findme.repository;

import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.OtpCode;
import com.geolink.findme.entity.OtpPurpose;
import com.geolink.findme.entity.User;
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
 * Tests d'intégration JPA du repository {@link OtpCodeRepository}.
 * Vérifie le stockage des codes OTP, le filtrage par statut consumé et la recherche par date de création.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testotpdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Tests d'intégration JPA - OtpCodeRepository")
class OtpCodeRepositoryJpaTest {

    @Autowired
    private OtpCodeRepository otpCodeRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        otpCodeRepository.deleteAll();
        userRepository.deleteAll();

        user = User.builder()
                .email("otp.user@geolink.com")
                .fullName("Otp User")
                .passwordHash("hashed")
                .status(AccountStatus.ACTIVE)
                .accountVerified(false)
                .build();
        userRepository.save(user);

        OtpCode oldCode = OtpCode.builder()
                .user(user)
                .codeHash("hash_old_123456")
                .purpose(OtpPurpose.ACCOUNT_VERIFICATION)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .consumed(false)
                .createdAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .build();

        OtpCode recentCode = OtpCode.builder()
                .user(user)
                .codeHash("hash_recent_654321")
                .purpose(OtpPurpose.ACCOUNT_VERIFICATION)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .consumed(false)
                .createdAt(Instant.now())
                .build();

        otpCodeRepository.save(oldCode);
        otpCodeRepository.save(recentCode);
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Deux codes OTP non consommés générés successivement pour le même utilisateur.
     * 2. When   : Recherche du code le plus récent non consommé.
     * 3. Then   : La méthode doit renvoyer `hash_recent_654321`.
     */
    @Test
    @DisplayName("Devrait récupérer le dernier code OTP non consommé d'un utilisateur")
    void devrait_trouver_dernier_code_otp_non_consomme() {
        Optional<OtpCode> latest = otpCodeRepository.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                user.getId(),
                OtpPurpose.ACCOUNT_VERIFICATION
        );

        assertThat(latest).isPresent();
        assertThat(latest.get().getCodeHash()).isEqualTo("hash_recent_654321");
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Des codes OTP actifs pour l'utilisateur.
     * 2. When   : Exécution de `consumeAllActiveForUserAndPurpose`.
     * 3. Then   : Tous les codes actifs pour cette utilisation doivent être marqués `consumed = true`.
     */
    @Test
    @DisplayName("Devrait consommer tous les codes OTP actifs d'un utilisateur pour une fin donnée")
    void devrait_consommer_tous_les_codes_otp_actifs() {
        otpCodeRepository.consumeAllActiveForUserAndPurpose(user.getId(), OtpPurpose.ACCOUNT_VERIFICATION);

        Optional<OtpCode> latest = otpCodeRepository.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                user.getId(),
                OtpPurpose.ACCOUNT_VERIFICATION
        );

        assertThat(latest).isEmpty();
    }
}
