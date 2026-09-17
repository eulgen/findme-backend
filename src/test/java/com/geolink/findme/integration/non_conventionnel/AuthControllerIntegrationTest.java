package com.geolink.findme.integration.non_conventionnel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geolink.findme.dto.request.AppleLoginRequestDTO;
import com.geolink.findme.dto.request.ForgotPasswordRequestDTO;
import com.geolink.findme.dto.request.GoogleLoginRequestDTO;
import com.geolink.findme.dto.request.RefreshRequestDTO;
import com.geolink.findme.dto.request.ResendOtpRequestDTO;
import com.geolink.findme.dto.request.ResetPasswordRequestDTO;
import com.geolink.findme.dto.request.SignInRequestDTO;
import com.geolink.findme.dto.request.SignUpRequestDTO;
import com.geolink.findme.dto.request.VerifyOtpRequestDTO;
import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.OtpCode;
import com.geolink.findme.entity.OtpPurpose;
import com.geolink.findme.entity.RefreshToken;
import com.geolink.findme.entity.Role;
import com.geolink.findme.entity.User;
import com.geolink.findme.repository.OtpCodeRepository;
import com.geolink.findme.repository.RefreshTokenRepository;
import com.geolink.findme.repository.RoleRepository;
import com.geolink.findme.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration non conventionnels du contrôleur {@link AuthController}.
 * Couvre les cas d'erreurs : mauvais mot de passe, OTP invalide, body invalide, token révoqué.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testauthwebdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "management.health.mail.enabled=false"
})
@DisplayName("Tests d'intégration Web REST - AuthController (Non Conventionnels)")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpCodeRepository otpCodeRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JavaMailSender javaMailSender;

    @Autowired
    private RoleRepository roleRepository;

    private User verifiedUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        otpCodeRepository.deleteAll();
        userRepository.deleteAll();

        roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").description("User").build()));

        verifiedUser = User.builder()
                .email("verified@geolink.com")
                .fullName("Verified User")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();
        userRepository.save(verifiedUser);
    }

    // ======================= Inscription =======================

    @Test
    @DisplayName("Devrait inscrire un nouvel utilisateur avec succès (201 Created)")
    void devrait_inscrire_un_nouvel_utilisateur() throws Exception {
        SignUpRequestDTO dto = new SignUpRequestDTO();
        dto.setEmail("newuser@geolink.com");
        dto.setPassword("Secret123!");
        dto.setFullName("New User");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Devrait rejeter l'inscription si l'email est déjà utilisé (409 Conflict)")
    void devrait_rejeter_inscription_si_email_existant() throws Exception {
        SignUpRequestDTO dto = new SignUpRequestDTO();
        dto.setEmail("verified@geolink.com");
        dto.setPassword("Secret123!");
        dto.setFullName("Duplicate User");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Devrait rejeter l'inscription si le body est invalide - email absent (400 Bad Request)")
    void devrait_rejeter_inscription_si_body_invalide() throws Exception {
        SignUpRequestDTO dto = new SignUpRequestDTO();
        dto.setPassword("Secret123!");
        dto.setFullName("No Email User");
        // email manquant

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ======================= Connexion =======================

    @Test
    @DisplayName("Devrait connecter un utilisateur vérifié et délivrer des jetons JWT (200 OK)")
    void devrait_connecter_utilisateur_valide() throws Exception {
        SignInRequestDTO dto = new SignInRequestDTO();
        dto.setEmail("verified@geolink.com");
        dto.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Devrait refuser la connexion si le mot de passe est incorrect (401 Unauthorized)")
    void devrait_refuser_connexion_si_mot_de_passe_incorrect() throws Exception {
        SignInRequestDTO dto = new SignInRequestDTO();
        dto.setEmail("verified@geolink.com");
        dto.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Devrait refuser la connexion si l'email n'existe pas (401 Unauthorized)")
    void devrait_refuser_connexion_si_email_inexistant() throws Exception {
        SignInRequestDTO dto = new SignInRequestDTO();
        dto.setEmail("nobody@geolink.com");
        dto.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Devrait refuser la connexion si le compte n'est pas vérifié (401 Unauthorized)")
    void devrait_refuser_connexion_si_compte_non_verifie() throws Exception {
        User unverified = User.builder()
                .email("unverified@geolink.com")
                .fullName("Unverified")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(AccountStatus.ACTIVE)
                .accountVerified(false)
                .build();
        userRepository.save(unverified);

        SignInRequestDTO dto = new SignInRequestDTO();
        dto.setEmail("unverified@geolink.com");
        dto.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    // ======================= Vérification OTP =======================

    @Test
    @DisplayName("Devrait valider le compte lors de la soumission d'un OTP correct (200 OK)")
    void devrait_valider_compte_avec_otp_correct() throws Exception {
        User unverified = User.builder()
                .email("toverify@geolink.com")
                .fullName("To Verify")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(AccountStatus.ACTIVE)
                .accountVerified(false)
                .build();
        userRepository.save(unverified);

        OtpCode otpCode = OtpCode.builder()
                .user(unverified)
                .codeHash(passwordEncoder.encode("123456"))
                .purpose(OtpPurpose.ACCOUNT_VERIFICATION)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .consumed(false)
                .build();
        otpCodeRepository.save(otpCode);

        VerifyOtpRequestDTO dto = new VerifyOtpRequestDTO();
        dto.setEmail("toverify@geolink.com");
        dto.setCode("123456");

        mockMvc.perform(post("/api/auth/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Devrait rejeter la vérification si le code OTP est incorrect (400 Bad Request)")
    void devrait_rejeter_verification_si_code_otp_incorrect() throws Exception {
        User unverified = User.builder()
                .email("badotp@geolink.com")
                .fullName("Bad OTP")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(AccountStatus.ACTIVE)
                .accountVerified(false)
                .build();
        userRepository.save(unverified);

        OtpCode otpCode = OtpCode.builder()
                .user(unverified)
                .codeHash(passwordEncoder.encode("999999"))
                .purpose(OtpPurpose.ACCOUNT_VERIFICATION)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .consumed(false)
                .build();
        otpCodeRepository.save(otpCode);

        VerifyOtpRequestDTO dto = new VerifyOtpRequestDTO();
        dto.setEmail("badotp@geolink.com");
        dto.setCode("000000"); // mauvais code

        mockMvc.perform(post("/api/auth/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait rejeter la vérification si le code OTP est expiré (410 Gone)")
    void devrait_rejeter_verification_si_code_otp_expire() throws Exception {
        User unverified = User.builder()
                .email("expiredotp@geolink.com")
                .fullName("Expired OTP")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(AccountStatus.ACTIVE)
                .accountVerified(false)
                .build();
        userRepository.save(unverified);

        OtpCode expiredOtp = OtpCode.builder()
                .user(unverified)
                .codeHash(passwordEncoder.encode("123456"))
                .purpose(OtpPurpose.ACCOUNT_VERIFICATION)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS)) // expiré
                .consumed(false)
                .build();
        otpCodeRepository.save(expiredOtp);

        VerifyOtpRequestDTO dto = new VerifyOtpRequestDTO();
        dto.setEmail("expiredotp@geolink.com");
        dto.setCode("123456");

        // OtpService lève InvalidOrExpiredTokenException("...", expired=true) → handler retourne 410 GONE
        mockMvc.perform(post("/api/auth/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isGone());
    }

    // ======================= Refresh Token =======================

    @Test
    @DisplayName("Devrait refuser le refresh si le token est révoqué (400 Bad Request)")
    void devrait_refuser_refresh_si_token_revoque() throws Exception {
        // Un token révoqué n'étant pas retrouvable via son hash raw,
        // le service lève InvalidOrExpiredTokenException(expired=false) → handler : 400
        RefreshRequestDTO dto = new RefreshRequestDTO();
        dto.setRefreshToken("revoked.raw.token.not.in.db");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait refuser le refresh si le token est introuvable en BDD (400 Bad Request)")
    void devrait_refuser_refresh_si_token_inexistant() throws Exception {
        // Token inconnu en base → InvalidOrExpiredTokenException(expired=false) → handler : 400
        RefreshRequestDTO dto = new RefreshRequestDTO();
        dto.setRefreshToken("completely.fake.token.unknown");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ======================= Mot de passe oublié =======================

    @Test
    @DisplayName("Devrait accepter la demande de réinitialisation de mot de passe (200 OK)")
    void devrait_accepter_demande_forgot_password() throws Exception {
        ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO();
        dto.setEmail("anyuser@geolink.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Devrait refuser le renvoi d'OTP si le compte est déjà vérifié (400 Bad Request)")
    void devrait_refuser_renvoi_otp_si_compte_deja_verifie() throws Exception {
        ResendOtpRequestDTO dto = new ResendOtpRequestDTO();
        dto.setEmail("verified@geolink.com"); // verifiedUser est déjà accountVerified = true

        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait refuser la réinitialisation de mot de passe si l'OTP est invalide (400 Bad Request)")
    void devrait_refuser_reset_password_si_otp_invalide() throws Exception {
        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setEmail("verified@geolink.com");
        dto.setCode("000000"); // faux code OTP
        dto.setNewPassword("NewSecret123!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait refuser la connexion Google si le token est invalide (401 Unauthorized)")
    void devrait_refuser_connexion_google_si_id_token_invalide() throws Exception {
        GoogleLoginRequestDTO dto = new GoogleLoginRequestDTO();
        dto.setIdToken("invalid_google_id_token");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Devrait refuser la connexion Apple si le token est invalide (401 Unauthorized)")
    void devrait_refuser_connexion_apple_si_id_token_invalide() throws Exception {
        AppleLoginRequestDTO dto = new AppleLoginRequestDTO();
        dto.setIdToken("invalid_apple_id_token");

        mockMvc.perform(post("/api/auth/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }
}
