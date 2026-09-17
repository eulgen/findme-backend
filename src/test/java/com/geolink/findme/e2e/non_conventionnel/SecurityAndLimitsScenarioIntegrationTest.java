package com.geolink.findme.e2e.non_conventionnel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geolink.findme.dto.request.AddressRequestDTO;
import com.geolink.findme.dto.request.SignInRequestDTO;
import com.geolink.findme.dto.request.VerifyOtpRequestDTO;
import com.geolink.findme.dto.response.GpsCoordinateDTO;
import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.Address;
import com.geolink.findme.entity.OtpCode;
import com.geolink.findme.entity.OtpPurpose;
import com.geolink.findme.entity.Role;
import com.geolink.findme.entity.User;
import com.geolink.findme.repository.AddressRepository;
import com.geolink.findme.repository.OtpCodeRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration de scénarios aux limites, d'erreurs et de violation de sécurité.
 * Vérifie le comportement applicatif face aux attaques, accès non autorisés et erreurs métiers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testscenar4db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
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
@DisplayName("Scénario d'intégration - Limites, Erreurs et Sécurité")
class SecurityAndLimitsScenarioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OtpCodeRepository otpCodeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private JavaMailSender javaMailSender;

    private User normalUser;
    private User victimUser;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        otpCodeRepository.deleteAll();
        userRepository.deleteAll();

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").description("User").build()));

        normalUser = User.builder()
                .email("user.limit@geolink.com")
                .fullName("User Limit")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();
        normalUser.getRoles().add(userRole);
        userRepository.save(normalUser);

        victimUser = User.builder()
                .email("victim@geolink.com")
                .fullName("Victim User")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();
        victimUser.getRoles().add(userRole);
        userRepository.saveAndFlush(victimUser);
    }

    /**
     * CAS D'ERREUR 1 : Dépassement de la limite de 4 adresses.
     * 1. Given  : L'utilisateur possède déjà 4 adresses en BDD.
     * 2. When   : Tentative de création d'une 5ème adresse.
     * 3. Then   : Rejet avec le statut 409 CONFLICT.
     */
    @Test
    @DisplayName("Cas d'Erreur : Rejet de la 5ème adresse (409 Conflict)")
    void devrait_rejeter_cinquieme_adresse() throws Exception {
        for (int i = 1; i <= 4; i++) {
            Address addr = Address.builder()
                    .addressCode("ADR-LIMIT-" + i)
                    .country("France")
                    .city("Paris")
                    .district("Quartier " + i)
                    .build();
            addr.getUsers().add(normalUser);
            addressRepository.save(addr);
            normalUser.getAddresses().add(addr);
        }
        userRepository.saveAndFlush(normalUser);

        UserPrincipal principal = new UserPrincipal(normalUser);

        AddressRequestDTO fifthAddr = new AddressRequestDTO();
        fifthAddr.setCountry("France");
        fifthAddr.setCity("Paris");
        fifthAddr.setDistrict("Quartier 5");
        fifthAddr.setStreet("Rue de la limite");
        fifthAddr.setPhotoUrl("https://cdn.findme.com/photo5.jpg");
        fifthAddr.setGps(new GpsCoordinateDTO(48.85, 2.35));

        mockMvc.perform(post("/api/addresses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fifthAddr)))
                .andExpect(status().isConflict());
    }

    /**
     * CAS DE SÉCURITÉ 2 : Tentative d'accès/suppression de l'adresse d'un autre utilisateur.
     * 1. Given  : La `victimUser` possède une adresse.
     * 2. When   : `normalUser` tente d'accéder à l'adresse de `victimUser` (GET /api/addresses/{id}).
     * 3. Then   : Rejet avec le statut 403 FORBIDDEN.
     */
    @Test
    @DisplayName("Cas de Sécurité : Refus d'accès à l'adresse d'un tiers (403 Forbidden)")
    void devrait_refuser_accès_adresse_d_un_tiers() throws Exception {
        Address victimAddr = Address.builder()
                .addressCode("ADR-VICTIM-1")
                .country("France")
                .city("Marseille")
                .district("Vieux-Port")
                .build();
        victimAddr.getUsers().add(victimUser);
        addressRepository.save(victimAddr);
        victimUser.getAddresses().add(victimAddr);
        userRepository.saveAndFlush(victimUser);

        UserPrincipal normalPrincipal = new UserPrincipal(normalUser);

        mockMvc.perform(delete("/api/addresses/" + victimAddr.getId())
                        .with(user(normalPrincipal)))
                .andExpect(status().isForbidden());
    }

    /**
     * CAS D'ERREUR 3 : Mot de passe incorrect lors de la connexion.
     * 1. Given  : Utilisateur existant en BDD.
     * 2. When   : Connexion avec un mauvais mot de passe.
     * 3. Then   : Status 401 UNAUTHORIZED.
     */
    @Test
    @DisplayName("Cas d'Erreur : Connexion avec un mauvais mot de passe (401 Unauthorized)")
    void devrait_refuser_connexion_si_mauvais_mot_de_passe() throws Exception {
        SignInRequestDTO dto = new SignInRequestDTO();
        dto.setEmail("user.limit@geolink.com");
        dto.setPassword("WrongPassword999!");

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * CAS D'ERREUR 4 : Validation avec un code OTP erroné.
     * 1. Given  : Un code OTP est généré en BDD.
     * 2. When   : Soumission d'un code OTP erroné "000000".
     * 3. Then   : Status 400 BAD REQUEST.
     */
    @Test
    @DisplayName("Cas d'Erreur : Validation OTP avec un mauvais code (400 Bad Request)")
    void devrait_refuser_validation_avec_mauvais_otp() throws Exception {
        OtpCode otp = OtpCode.builder()
                .user(normalUser)
                .codeHash(passwordEncoder.encode("123456"))
                .purpose(OtpPurpose.ACCOUNT_VERIFICATION)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .consumed(false)
                .build();
        otpCodeRepository.saveAndFlush(otp);

        VerifyOtpRequestDTO dto = new VerifyOtpRequestDTO();
        dto.setEmail("user.limit@geolink.com");
        dto.setCode("000000"); // Mauvais code

        mockMvc.perform(post("/api/auth/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
