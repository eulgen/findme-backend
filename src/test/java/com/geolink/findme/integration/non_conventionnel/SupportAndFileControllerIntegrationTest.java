package com.geolink.findme.integration.non_conventionnel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geolink.findme.dto.request.CreateSupportRequestDTO;
import com.geolink.findme.entity.SupportMessage;
import com.geolink.findme.entity.SupportStatus;
import com.geolink.findme.repository.SupportMessageRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration non conventionnels des contrôleurs {@link SupportController} et {@link FileController}.
 * Couvre les cas d'erreurs : body invalide (email null, message vide).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testsupportwebdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "management.health.mail.enabled=false"
})
@DisplayName("Tests d'intégration Web REST - Support & File Controllers (Non Conventionnels)")
class SupportAndFileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SupportMessageRepository supportMessageRepository;

    @BeforeEach
    void setUp() {
        supportMessageRepository.deleteAll();
    }

    // ======================= Support =======================

    @Test
    @DisplayName("Devrait créer une demande de support client (201 Created)")
    void devrait_creer_demande_support() throws Exception {
        CreateSupportRequestDTO dto = new CreateSupportRequestDTO();
        dto.setName("Visiteur Anonyme");
        dto.setEmail("visiteur@geolink.com");
        dto.setMessage("Bonjour, comment obtenir un code d'adresse ?");

        mockMvc.perform(post("/api/support")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("visiteur@geolink.com"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Devrait refuser la création d'un message de support si l'email est absent (400 Bad Request)")
    void devrait_rejeter_support_si_email_absent() throws Exception {
        CreateSupportRequestDTO dto = new CreateSupportRequestDTO();
        dto.setName("Visiteur Sans Email");
        // email manquant intentionnellement
        dto.setMessage("Message sans email.");

        mockMvc.perform(post("/api/support")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait refuser la création d'un message de support si le message est vide (400 Bad Request)")
    void devrait_rejeter_support_si_message_vide() throws Exception {
        CreateSupportRequestDTO dto = new CreateSupportRequestDTO();
        dto.setName("Visiteur");
        dto.setEmail("visiteur@geolink.com");
        dto.setMessage(""); // message vide

        mockMvc.perform(post("/api/support")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ======================= FileController =======================

    @Test
    @DisplayName("Devrait retourner 400 Bad Request si le fichier demandé n'existe pas sur le serveur")
    void devrait_retourner_400_si_fichier_inexistant() throws Exception {
        mockMvc.perform(get("/api/files/addresses/non_existent_photo.jpg"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait rejeter toute tentative de Path Traversal dans l'URL de fichier")
    void devrait_rejeter_path_traversal_dans_url_fichier() throws Exception {
        mockMvc.perform(get("/api/files/addresses/..%2F..%2Fapplication.properties"))
                .andExpect(status().isBadRequest());
    }
}

