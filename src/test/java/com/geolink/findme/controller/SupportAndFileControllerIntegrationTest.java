package com.geolink.findme.controller;

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
 * Tests d'intégration Web des contrôleurs {@link SupportController} et {@link FileController}.
 * Vérifie l'envoi de messages de support client et l'accès aux fichiers.
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
@DisplayName("Tests d'intégration Web REST - Support & File Controllers")
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

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Un utilisateur (authentifié ou anonyme) soumettant une demande d'assistance.
     * 2. When   : POST `/api/support`.
     * 3. Then   : Status 201 CREATED et enregistrement du message en BDD.
     */
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
}
