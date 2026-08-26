package com.geolink.findme.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geolink.findme.dto.request.RefreshRequestDTO;
import com.geolink.findme.dto.request.SignInRequestDTO;
import com.geolink.findme.dto.request.SignUpRequestDTO;
import com.geolink.findme.dto.request.UpdateProfileRequestDTO;
import com.geolink.findme.dto.request.VerifyOtpRequestDTO;
import com.geolink.findme.entity.OtpCode;
import com.geolink.findme.entity.OtpPurpose;
import com.geolink.findme.entity.Role;
import com.geolink.findme.entity.User;
import com.geolink.findme.repository.OtpCodeRepository;
import com.geolink.findme.repository.RefreshTokenRepository;
import com.geolink.findme.repository.RoleRepository;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.security.UserPrincipal;
import jakarta.persistence.EntityManager;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration de scénario fonctionnel complet : Cycle de vie d'un utilisateur.
 * Enchaîne : Inscription -> Validation OTP -> Connexion -> Consultation Profil -> Modification -> Refresh Token -> Déconnexion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testscenar1db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "management.health.mail.enabled=false",
        "app.otp.ttl-minutes=60",
        "securite.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
        "securite.jwt.expiration-minutes=60",
        "securite.jwt.refresh-expiration-days=7"
})
@DisplayName("Scénario d'intégration - Cycle de vie complet de l'utilisateur")
class FullUserLifecycleScenarioIntegrationTest {

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
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        otpCodeRepository.deleteAll();
        userRepository.deleteAll();

        roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").description("User").build()));
    }

    /**
     * LOGIQUE DU SCÉNARIO NOMINAL (HAPPY PATH) :
     * Étape 1 : Un visiteur soumet le formulaire d'inscription (POST /api/auth/signup).
     * Étape 2 : L'OTP est extrait de la BDD et soumis pour valider le compte (POST /api/auth/verify-account).
     * Étape 3 : L'utilisateur se connecte (POST /api/auth/signin) et récupère ses jetons JWT.
     * Étape 4 : L'utilisateur consulte son profil (GET /api/users/me).
     * Étape 5 : L'utilisateur met à jour son profil (PUT /api/users/me).
     * Étape 6 : L'utilisateur effectue une rotation de son refresh token (POST /api/auth/refresh).
     * Étape 7 : L'utilisateur se déconnecte (POST /api/auth/logout).
     */
    @Test
    @DisplayName("Scénario Nominal : Inscription -> OTP -> Signin -> Profil -> Refresh -> Logout")
    void devrait_erecuter_le_scenario_complet_de_vie_utilisateur() throws Exception {
        String email = "lifecycle.user@geolink.com";
        String password = "Password123!";

        // 1. INSCRIPTION
        SignUpRequestDTO signUpReq = new SignUpRequestDTO();
        signUpReq.setEmail(email);
        signUpReq.setPassword(password);
        signUpReq.setFullName("Lifecycle User");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));

        // 2. VALIDATION OTP
        User userInDb = userRepository.findByEmail(email).orElseThrow();
        OtpCode otpCode = otpCodeRepository.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                userInDb.getId(), OtpPurpose.ACCOUNT_VERIFICATION
        ).orElseThrow();

        otpCode.setCodeHash(passwordEncoder.encode("654321"));
        otpCode.setExpiresAt(Instant.now().plus(2, java.time.temporal.ChronoUnit.HOURS));
        otpCodeRepository.saveAndFlush(otpCode);
        entityManager.clear(); // Vide le cache L1 pour forcer la relecture du nouvel hash OTP

        VerifyOtpRequestDTO verifyReq = new VerifyOtpRequestDTO();
        verifyReq.setEmail(email);
        verifyReq.setCode("654321");

        mockMvc.perform(post("/api/auth/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk());

        User verifiedUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(verifiedUser.isAccountVerified()).isTrue();

        // 3. CONNEXION
        SignInRequestDTO signInReq = new SignInRequestDTO();
        signInReq.setEmail(email);
        signInReq.setPassword(password);

        MvcResult signInResult = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode tokensNode = objectMapper.readTree(signInResult.getResponse().getContentAsString());
        String refreshTokenStr = tokensNode.get("refreshToken").asText();

        // 4. CONSULTATION PROFIL
        UserPrincipal principal = new UserPrincipal(verifiedUser);
        mockMvc.perform(get("/api/users/me").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // 5. MISE À JOUR PROFIL
        UpdateProfileRequestDTO updateReq = new UpdateProfileRequestDTO();
        updateReq.setFullName("Lifecycle User Updated");
        updateReq.setPhoneNumber("+33611223344");

        mockMvc.perform(put("/api/users/me")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Lifecycle User Updated"));

        // 6. REFRESH TOKEN
        RefreshRequestDTO refreshReq = new RefreshRequestDTO();
        refreshReq.setRefreshToken(refreshTokenStr);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // 7. DÉCONNEXION
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isNoContent());
    }
}
