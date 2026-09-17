package com.geolink.findme.integration.non_conventionnel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geolink.findme.dto.request.UpdateProfileRequestDTO;
import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.User;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration non conventionnels du contrôleur {@link UserController}.
 * Couvre les cas d'erreurs : body invalide, upload interdit, accès sans authentification.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testuserwebdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "management.health.mail.enabled=false"
})
@DisplayName("Tests d'intégration Web REST - UserController (Non Conventionnels)")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = User.builder()
                .email("profile.user@geolink.com")
                .fullName("John Doe")
                .phoneNumber("+33699999999")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();
        userRepository.save(testUser);
    }

    // ======================= Consultation profil =======================

    @Test
    @DisplayName("Devrait retourner les détails du profil de l'utilisateur connecté (200 OK)")
    void devrait_retourner_profil_utilisateur_connecte() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .with(user(new UserPrincipal(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile.user@geolink.com"))
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    @DisplayName("Devrait refuser l'accès au profil sans authentification (401 Unauthorized)")
    void devrait_refuser_acces_profil_sans_authentification() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ======================= Modification profil =======================

    @Test
    @DisplayName("Devrait mettre à jour les informations du profil utilisateur (200 OK)")
    void devrait_mettre_a_jour_profil_utilisateur() throws Exception {
        UpdateProfileRequestDTO dto = new UpdateProfileRequestDTO();
        dto.setFullName("John Updated");
        dto.setPhoneNumber("+33688888888");

        mockMvc.perform(put("/api/users/me")
                        .with(user(new UserPrincipal(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Updated"))
                .andExpect(jsonPath("$.phoneNumber").value("+33688888888"));
    }

    @Test
    @DisplayName("Devrait refuser la mise à jour du profil avec un fullName vide (400 Bad Request)")
    void devrait_refuser_mise_a_jour_profil_si_fullname_vide() throws Exception {
        UpdateProfileRequestDTO dto = new UpdateProfileRequestDTO();
        dto.setFullName("   "); // blanc uniquement
        dto.setPhoneNumber("+33688888888");

        mockMvc.perform(put("/api/users/me")
                        .with(user(new UserPrincipal(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait refuser la mise à jour du profil sans authentification (401 Unauthorized)")
    void devrait_refuser_mise_a_jour_profil_sans_authentification() throws Exception {
        UpdateProfileRequestDTO dto = new UpdateProfileRequestDTO();
        dto.setFullName("Ghost");

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    // ======================= Upload photo de profil =======================

    @Test
    @DisplayName("Devrait refuser l'upload d'une photo avec un format non autorisé - PDF (400 Bad Request)")
    void devrait_refuser_upload_photo_profil_format_non_autorise() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/users/me/profile-image")
                        .file(pdfFile)
                        .with(user(new UserPrincipal(testUser))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait refuser l'upload d'une photo de profil supérieure à 5 Mo (400 Bad Request)")
    void devrait_refuser_upload_photo_profil_taille_trop_grande() throws Exception {
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6 Mo
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large_photo.jpg",
                "image/jpeg",
                largeContent
        );

        mockMvc.perform(multipart("/api/users/me/profile-image")
                        .file(largeFile)
                        .with(user(new UserPrincipal(testUser))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait refuser l'upload de photo de profil sans authentification (401 Unauthorized)")
    void devrait_refuser_upload_photo_profil_sans_authentification() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                "fake content".getBytes()
        );

        mockMvc.perform(multipart("/api/users/me/profile-image")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }
}
