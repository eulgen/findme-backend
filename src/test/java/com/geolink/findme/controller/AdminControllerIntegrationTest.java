package com.geolink.findme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geolink.findme.dto.request.RoleUpdateDTO;
import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.Role;
import com.geolink.findme.entity.User;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration Web et Sécurité RBAC du contrôleur {@link AdminUserController}.
 * Vérifie les autorisations basées sur les rôles (`ADMIN` autorisé, `USER` refusé 403).
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
@DisplayName("Tests d'intégration Web REST - AdminUserController")
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User adminUser;
    private User normalUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // En BDD, le nom du rôle est "ADMIN" (UserPrincipal y ajoute automatiquement le préfixe "ROLE_")
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

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Un administrateur connecté avec le rôle `ADMIN` (qui produit l'autorité `ROLE_ADMIN`).
     * 2. When   : GET `/api/admin/users`.
     * 3. Then   : Status 200 OK avec la liste paginée de tous les utilisateurs.
     */
    @Test
    @DisplayName("Devrait autoriser l'accès à la liste des utilisateurs pour un administrateur (200 OK)")
    void devrait_autoriser_liste_utilisateurs_pour_admin() throws Exception {
        UserPrincipal adminPrincipal = new UserPrincipal(adminUser);

        mockMvc.perform(get("/api/admin/users")
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Un utilisateur standard sans privilèges administrateur (`USER`).
     * 2. When   : GET `/api/admin/users`.
     * 3. Then   : Status 403 FORBIDDEN.
     */
    @Test
    @DisplayName("Devrait refuser l'accès d'administration à un utilisateur standard (403 Forbidden)")
    void devrait_refuser_accès_admin_pour_utilisateur_standard() throws Exception {
        UserPrincipal userPrincipal = new UserPrincipal(normalUser);

        mockMvc.perform(get("/api/admin/users")
                        .with(user(userPrincipal)))
                .andExpect(status().isForbidden());
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Un administrateur souhaitant promouvoir un utilisateur au rôle SUPPORT_AGENT.
     * 2. When   : PUT `/api/admin/users/{userId}/role`.
     * 3. Then   : Status 200 OK.
     */
    @Test
    @DisplayName("Devrait permettre à un administrateur de changer le rôle d'un utilisateur (200 OK)")
    void devrait_permettre_a_l_admin_de_changer_un_role() throws Exception {
        roleRepository.findByName("SUPPORT_AGENT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("SUPPORT_AGENT").description("Support").build()));

        UserPrincipal adminPrincipal = new UserPrincipal(adminUser);

        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setRoleName("SUPPORT_AGENT");

        mockMvc.perform(put("/api/admin/users/" + normalUser.getId() + "/role")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}
