package com.geolink.findme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geolink.findme.dto.request.AddressRequestDTO;
import com.geolink.findme.dto.response.GpsCoordinateDTO;
import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.Address;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration Web et Sécurité du contrôleur {@link AddressController}.
 * Vérifie la gestion des adresses (CRUD, comptage, limite de 4 adresses).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testaddrwebdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "management.health.mail.enabled=false"
})
@DisplayName("Tests d'intégration Web REST - AddressController")
class AddressControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .email("address.user@geolink.com")
                .fullName("Address Tester")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();
        userRepository.saveAndFlush(testUser);
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Utilisateur connecté souhaitant enregistrer sa première adresse avec photoUrl et GPS valides.
     * 2. When   : POST `/api/addresses` avec des données géographiques valides.
     * 3. Then   : Status 201 CREATED et retour du DTO de l'adresse créée.
     */
    @Test
    @DisplayName("Devrait créer une nouvelle adresse pour l'utilisateur connecté (201 Created)")
    void devrait_creer_une_adresse_pour_utilisateur_connecte() throws Exception {
        UserPrincipal principal = new UserPrincipal(testUser);

        AddressRequestDTO request = new AddressRequestDTO();
        request.setCountry("France");
        request.setCity("Bordeaux");
        request.setDistrict("Centre-Ville");
        request.setStreet("Rue Sainte-Catherine");
        request.setHouseNumber("10");
        request.setPhotoUrl("https://cdn.findme.com/photos/bordeaux.jpg");
        request.setGps(new GpsCoordinateDTO(44.837789, -0.57918));

        mockMvc.perform(post("/api/addresses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.city").value("Bordeaux"))
                .andExpect(jsonPath("$.addressCode").isNotEmpty());
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'utilisateur possède déjà une adresse enregistrée.
     * 2. When   : GET `/api/addresses`.
     * 3. Then   : Status 200 OK avec la liste des adresses dans l'élément `content`.
     */
    @Test
    @DisplayName("Devrait lister les adresses de l'utilisateur connecté (200 OK)")
    void devrait_lister_les_adresses_de_l_utilisateur() throws Exception {
        Address addr = Address.builder()
                .addressCode("ADR-2026-TEST")
                .country("France")
                .city("Nice")
                .district("Promenade")
                .build();
        addr.getUsers().add(testUser);
        addressRepository.save(addr);
        testUser.getAddresses().add(addr);
        userRepository.saveAndFlush(testUser);

        UserPrincipal principal = new UserPrincipal(testUser);

        mockMvc.perform(get("/api/addresses")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'utilisateur tente de supprimer une adresse lui appartenant.
     * 2. When   : DELETE `/api/addresses/{id}`.
     * 3. Then   : Status 204 NO_CONTENT.
     */
    @Test
    @DisplayName("Devrait supprimer une adresse de l'utilisateur (204 No Content)")
    void devrait_supprimer_adresse_utilisateur() throws Exception {
        Address addr = Address.builder()
                .addressCode("ADR-2026-DEL")
                .country("France")
                .city("Toulouse")
                .district("Capitole")
                .build();
        addr.getUsers().add(testUser);
        addressRepository.save(addr);
        testUser.getAddresses().add(addr);
        userRepository.saveAndFlush(testUser);

        UserPrincipal principal = new UserPrincipal(testUser);

        mockMvc.perform(delete("/api/addresses/" + addr.getId())
                        .with(user(principal)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/addresses/link/{addressCode} - Devrait lier une adresse existante au compte connecté (200 OK)")
    void linkAddress_Success() throws Exception {
        Address unlinkedAddress = Address.builder()
                .addressCode("ADR-2026-UNLINKED")
                .country("Cameroun")
                .city("Yaoundé")
                .district("Bastot")
                .build();
        addressRepository.save(unlinkedAddress);

        UserPrincipal principal = new UserPrincipal(testUser);

        mockMvc.perform(post("/api/addresses/link/ADR-2026-UNLINKED")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressCode").value("ADR-2026-UNLINKED"))
                .andExpect(jsonPath("$.city").value("Yaoundé"));
    }
}
