package com.geolink.findme.controller;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration Web et Sécurité du contrôleur {@link UserController}.
 * Vérifie l'accès au profil utilisateur connecté, sa modification et le blocage 401 si non authentifié.
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
@DisplayName("Tests d'intégration Web REST - UserController")
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

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Un utilisateur authentifié (`UserPrincipal` en contexte de sécurité).
     * 2. When   : GET `/api/users/me`.
     * 3. Then   : Status 200 OK et retour des détails du profil (John Doe).
     */
    @Test
    @DisplayName("Devrait retourner les détails du profil de l'utilisateur connecté (200 OK)")
    void devrait_retourner_profil_utilisateur_connecte() throws Exception {
        UserPrincipal principal = new UserPrincipal(testUser);

        mockMvc.perform(get("/api/users/me")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile.user@geolink.com"))
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Requête anonyme (sans jeton d'authentification).
     * 2. When   : GET `/api/users/me`.
     * 3. Then   : Status 401 UNAUTHORIZED.
     */
    @Test
    @DisplayName("Devrait refuser l'accès au profil sans authentification (401 Unauthorized)")
    void devrait_refuser_accès_profil_sans_authentification() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Un utilisateur connecté demandant une modification de son nom et téléphone.
     * 2. When   : PUT `/api/users/me` avec `UpdateProfileRequestDTO`.
     * 3. Then   : Status 200 OK et profil mis à jour.
     */
    @Test
    @DisplayName("Devrait mettre à jour les informations du profil utilisateur (200 OK)")
    void devrait_mettre_a_jour_profil_utilisateur() throws Exception {
        UserPrincipal principal = new UserPrincipal(testUser);

        UpdateProfileRequestDTO dto = new UpdateProfileRequestDTO();
        dto.setFullName("John Updated");
        dto.setPhoneNumber("+33688888888");

        mockMvc.perform(put("/api/users/me")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Updated"))
                .andExpect(jsonPath("$.phoneNumber").value("+33688888888"));
    }
}
