package com.geolink.findme.integration.non_conventionnel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geolink.findme.dto.request.RoleUpdateDTO;
import com.geolink.findme.dto.request.UpdateAddressStatusRequestDTO;
import com.geolink.findme.dto.request.UpdateSupportStatusRequestDTO;
import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.Address;
import com.geolink.findme.entity.AddressStatus;
import com.geolink.findme.entity.Role;
import com.geolink.findme.entity.SupportStatus;
import com.geolink.findme.entity.User;
import com.geolink.findme.repository.AddressRepository;
import com.geolink.findme.repository.RoleRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration non conventionnels du contrôleur {@link AdminUserController} et {@link AdminAddressController}.
 * Couvre les cas d'erreurs : rôle inexistant, utilisateur inexistant, adresse inexistante, accès interdit et non authentifié.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testadminwebdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "management.health.mail.enabled=false"
})
@DisplayName("Tests d'intégration Web REST - AdminController (Non Conventionnels)")
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AddressRepository addressRepository;

    private User adminUser;
    private User normalUser;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        userRepository.deleteAll();

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").description("Admin").build()));
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").description("User").build()));

        adminUser = User.builder()
                .email("admin@geolink.com")
                .fullName("System Admin")
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();
        adminUser.getRoles().add(adminRole);
        userRepository.save(adminUser);

        normalUser = User.builder()
                .email("normal@geolink.com")
                .fullName("Normal User")
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();
        normalUser.getRoles().add(userRole);
        userRepository.save(normalUser);
    }

    // ======================= RBAC =======================

    @Test
    @DisplayName("Devrait autoriser l'accès à la liste des utilisateurs pour un administrateur (200 OK)")
    void devrait_autoriser_liste_utilisateurs_pour_admin() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(user(new UserPrincipal(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Devrait refuser l'accès admin à un utilisateur standard (403 Forbidden)")
    void devrait_refuser_acces_admin_pour_utilisateur_standard() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(user(new UserPrincipal(normalUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Devrait refuser l'accès admin sans authentification (401 Unauthorized)")
    void devrait_refuser_acces_admin_sans_authentification() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    // ======================= Changement de rôle =======================

    @Test
    @DisplayName("Devrait permettre à un administrateur de changer le rôle d'un utilisateur (200 OK)")
    void devrait_permettre_a_l_admin_de_changer_un_role() throws Exception {
        roleRepository.findByName("SUPPORT_AGENT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("SUPPORT_AGENT").description("Support").build()));

        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setRoleName("SUPPORT_AGENT");

        mockMvc.perform(put("/api/admin/users/" + normalUser.getId() + "/role")
                        .with(user(new UserPrincipal(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Devrait retourner 404 si l'utilisateur cible est inexistant lors du changement de rôle")
    void devrait_retourner_404_si_utilisateur_inexistant_lors_changement_role() throws Exception {
        roleRepository.findByName("SUPPORT_AGENT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("SUPPORT_AGENT").description("Support").build()));

        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setRoleName("SUPPORT_AGENT");

        mockMvc.perform(put("/api/admin/users/99999/role")
                        .with(user(new UserPrincipal(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Devrait retourner 404 si le rôle cible n'existe pas en BDD")
    void devrait_retourner_404_si_role_inexistant() throws Exception {
        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setRoleName("ROLE_INEXISTANT");

        mockMvc.perform(put("/api/admin/users/" + normalUser.getId() + "/role")
                        .with(user(new UserPrincipal(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Devrait refuser le changement de rôle pour un utilisateur non admin (403 Forbidden)")
    void devrait_refuser_changement_role_pour_non_admin() throws Exception {
        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setRoleName("ADMIN");

        mockMvc.perform(put("/api/admin/users/" + normalUser.getId() + "/role")
                        .with(user(new UserPrincipal(normalUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // ======================= Suppression d'adresse admin =======================

    @Test
    @DisplayName("Devrait retourner 404 si l'adresse admin à supprimer est inexistante")
    void devrait_retourner_404_si_adresse_admin_inexistante_lors_suppression() throws Exception {
        mockMvc.perform(delete("/api/admin/addresses/99999")
                        .with(user(new UserPrincipal(adminUser))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Devrait retourner 404 si l'adresse admin à modifier n'existe pas")
    void devrait_retourner_404_si_adresse_admin_inexistante_lors_maj_statut() throws Exception {
        UpdateAddressStatusRequestDTO dto = new UpdateAddressStatusRequestDTO();
        dto.setStatus(AddressStatus.VALIDE);

        mockMvc.perform(patch("/api/admin/addresses/99999/status")
                        .with(user(new UserPrincipal(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Devrait refuser la modification du statut d'une adresse par un utilisateur non admin (403 Forbidden)")
    void devrait_refuser_maj_statut_adresse_pour_non_admin() throws Exception {
        UpdateAddressStatusRequestDTO dto = new UpdateAddressStatusRequestDTO();
        dto.setStatus(AddressStatus.VALIDE);

        mockMvc.perform(patch("/api/admin/addresses/1/status")
                        .with(user(new UserPrincipal(normalUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // ======================= Support admin =======================

    @Test
    @DisplayName("Devrait refuser l'accès aux messages de support admin pour un utilisateur non privilégie (403 Forbidden)")
    void devrait_refuser_acces_support_admin_pour_utilisateur_standard() throws Exception {
        mockMvc.perform(get("/api/admin/support")
                        .with(user(new UserPrincipal(normalUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Devrait retourner 404 lors de la mise à jour du statut d'un message de support inexistant")
    void devrait_retourner_404_si_message_support_inexistant() throws Exception {
        UpdateSupportStatusRequestDTO dto = new UpdateSupportStatusRequestDTO();
        dto.setStatus(SupportStatus.PROCESSED);

        mockMvc.perform(patch("/api/admin/support/99999")
                        .with(user(new UserPrincipal(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }
}
