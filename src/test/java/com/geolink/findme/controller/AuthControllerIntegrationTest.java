package com.geolink.findme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geolink.findme.dto.request.ForgotPasswordRequestDTO;
import com.geolink.findme.dto.request.SignInRequestDTO;
import com.geolink.findme.dto.request.SignUpRequestDTO;
import com.geolink.findme.dto.request.VerifyOtpRequestDTO;
import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.OtpCode;
import com.geolink.findme.entity.OtpPurpose;
import com.geolink.findme.entity.Role;
import com.geolink.findme.entity.User;
import com.geolink.findme.repository.OtpCodeRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration Web et Sécurité du contrôleur {@link AuthController}.
 * Vérifie les endpoints d'inscription, validation OTP, connexion et gestion des mots de passe.
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
@DisplayName("Tests d'intégration Web REST - AuthController")
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
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JavaMailSender javaMailSender;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        otpCodeRepository.deleteAll();
        userRepository.deleteAll();

        roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").description("User").build()));
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Un DTO d'inscription avec des données valides.
     * 2. When   : POST `/api/auth/signup`.
     * 3. Then   : Status 201 CREATED, profil utilisateur retourné avec email et statut non vérifié.
     */
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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@geolink.com"))
                .andExpect(jsonPath("$.fullName").value("New User"));
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'email "existing@geolink.com" est déjà enregistré en BDD.
     * 2. When   : POST `/api/auth/signup` avec le même email.
     * 3. Then   : Status 409 CONFLICT avec message d'erreur.
     */
    @Test
    @DisplayName("Devrait rejeter l'inscription si l'email existe déjà (409 Conflict)")
    void devrait_rejeter_inscription_si_email_existant() throws Exception {
        User existing = User.builder()
                .email("existing@geolink.com")
                .fullName("Existing User")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();
        userRepository.save(existing);

        SignUpRequestDTO dto = new SignUpRequestDTO();
        dto.setEmail("existing@geolink.com");
        dto.setPassword("Secret123!");
        dto.setFullName("Duplicate User");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Un utilisateur non vérifié avec un code OTP valide "123456" enregistré.
     * 2. When   : POST `/api/auth/verify-account` avec "123456".
     * 3. Then   : Status 200 OK.
     */
    @Test
    @DisplayName("Devrait valider le compte lors de la soumission d'un OTP correct (200 OK)")
    void devrait_valider_compte_avec_otp_correct() throws Exception {
        User user = User.builder()
                .email("unverified@geolink.com")
                .fullName("Unverified User")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(AccountStatus.ACTIVE)
                .accountVerified(false)
                .build();
        userRepository.save(user);

        OtpCode otpCode = OtpCode.builder()
                .user(user)
                .codeHash(passwordEncoder.encode("123456"))
                .purpose(OtpPurpose.ACCOUNT_VERIFICATION)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .consumed(false)
                .build();
        otpCodeRepository.save(otpCode);

        VerifyOtpRequestDTO dto = new VerifyOtpRequestDTO();
        dto.setEmail("unverified@geolink.com");
        dto.setCode("123456");

        mockMvc.perform(post("/api/auth/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Un utilisateur actif et vérifié en BDD avec mot de passe "Password123!".
     * 2. When   : POST `/api/auth/signin` avec ses identifiants exacts.
     * 3. Then   : Status 200 OK avec retour des tokens JWT (accessToken & refreshToken).
     */
    @Test
    @DisplayName("Devrait connecter un utilisateur vérifié et délivrer des jetons JWT (200 OK)")
    void devrait_connecter_utilisateur_valide() throws Exception {
        User user = User.builder()
                .email("verified@geolink.com")
                .fullName("Verified User")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();
        userRepository.save(user);

        SignInRequestDTO dto = new SignInRequestDTO();
        dto.setEmail("verified@geolink.com");
        dto.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Demande de réinitialisation de mot de passe.
     * 2. When   : POST `/api/auth/forgot-password`.
     * 3. Then   : Status 200 OK.
     */
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
}
