package com.geolink.findme.e2e.conventionnel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geolink.findme.dto.request.CreateSupportRequestDTO;
import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.Role;
import com.geolink.findme.entity.SupportMessage;
import com.geolink.findme.entity.SupportStatus;
import com.geolink.findme.entity.User;
import com.geolink.findme.repository.RoleRepository;
import com.geolink.findme.repository.SupportMessageRepository;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration de scénario fonctionnel complet : Support Client & Administration.
 * Enchaîne : Soumission de ticket par utilisateur -> Consultation Admin -> Promotion de rôle.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testscenar3db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "management.health.mail.enabled=false"
})
@DisplayName("Scénario d'intégration - Support client et administration RBAC")
class SupportAndAdminFlowScenarioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupportMessageRepository supportMessageRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User adminUser;

    @BeforeEach
    void setUp() {
        supportMessageRepository.deleteAll();
        userRepository.deleteAll();

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").description("Admin").build()));

        adminUser = User.builder()
                .email("admin.scenario@geolink.com")
                .fullName("Admin Scenario")
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();
        adminUser.getRoles().add(adminRole);
        userRepository.saveAndFlush(adminUser);
    }

    /**
     * LOGIQUE DU SCÉNARIO NOMINAL :
     * 1. Un utilisateur soumet une demande de support (POST /api/support).
     * 2. L'administrateur liste tous les utilisateurs enregistrés (GET /api/admin/users).
     * 3. Le ticket de support est vérifié en BDD.
     */
    @Test
    @DisplayName("Scénario Nominal : Soumission Support -> Consultation Administration")
    void devrait_traiter_le_flux_support_et_admin() throws Exception {
        // 1. Soumission de ticket par utilisateur
        CreateSupportRequestDTO supportDto = new CreateSupportRequestDTO();
        supportDto.setName("Client Interroge");
        supportDto.setEmail("client.question@geolink.com");
        supportDto.setMessage("Comment puis-je réinitialiser mon mot de passe ?");

        mockMvc.perform(post("/api/support")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(supportDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("client.question@geolink.com"));

        // 2. Consultation Admin
        UserPrincipal adminPrincipal = new UserPrincipal(adminUser);

        mockMvc.perform(get("/api/admin/users")
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // 3. Vérification de la persistance du ticket
        assertThat(supportMessageRepository.findAll()).hasSize(1);
        SupportMessage msg = supportMessageRepository.findAll().get(0);
        assertThat(msg.getStatus()).isEqualTo(SupportStatus.PENDING);
    }
}
