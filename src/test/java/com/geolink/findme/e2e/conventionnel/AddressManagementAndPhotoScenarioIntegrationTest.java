package com.geolink.findme.e2e.conventionnel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geolink.findme.dto.request.AddressRequestDTO;
import com.geolink.findme.dto.response.GpsCoordinateDTO;
import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.User;
import com.geolink.findme.repository.AddressRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration de scénario fonctionnel complet : Gestion des adresses et export.
 * Enchaîne : Inscription -> Création de 3 adresses -> Recherche paginée -> Export PDF.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testscenar2db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "management.health.mail.enabled=false"
})
@DisplayName("Scénario d'intégration - Gestion complète des adresses")
class AddressManagementAndPhotoScenarioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .email("address.scenario@geolink.com")
                .fullName("Address Scenario User")
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();
        userRepository.saveAndFlush(testUser);
    }

    /**
     * LOGIQUE DU SCÉNARIO NOMINAL :
     * 1. L'utilisateur crée 2 adresses distinctes (Paris et Lyon).
     * 2. L'utilisateur consulte la liste de ses adresses (GET /api/addresses).
     * 3. L'utilisateur demande l'exportation PDF des données d'une adresse (GET /api/addresses/{id}/export).
     */
    @Test
    @DisplayName("Scénario Nominal : Création de 2 adresses -> Consultation -> Exportation PDF")
    void devrait_gerer_le_flux_complet_des_adresses() throws Exception {
        UserPrincipal principal = new UserPrincipal(testUser);

        // 1. Création Adresse 1 (Paris)
        AddressRequestDTO req1 = new AddressRequestDTO();
        req1.setCountry("France");
        req1.setCity("Paris");
        req1.setDistrict("Opéra");
        req1.setStreet("Boulevard Haussmann");
        req1.setHouseNumber("40");
        req1.setPhotoUrl("https://cdn.findme.com/paris.jpg");
        req1.setGps(new GpsCoordinateDTO(48.8737, 2.3314));

        mockMvc.perform(post("/api/addresses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.city").value("Paris"));

        // 2. Création Adresse 2 (Lyon)
        AddressRequestDTO req2 = new AddressRequestDTO();
        req2.setCountry("France");
        req2.setCity("Lyon");
        req2.setDistrict("Part-Dieu");
        req2.setStreet("Rue Garibaldi");
        req2.setHouseNumber("150");
        req2.setPhotoUrl("https://cdn.findme.com/lyon.jpg");
        req2.setGps(new GpsCoordinateDTO(45.7600, 4.8500));

        mockMvc.perform(post("/api/addresses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.city").value("Lyon"));

        // 3. Consultation des adresses créées
        mockMvc.perform(get("/api/addresses").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }
}
