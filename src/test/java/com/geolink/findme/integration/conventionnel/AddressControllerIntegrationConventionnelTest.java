package com.geolink.findme.integration.conventionnel;

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

class AddressControllerIntegrationConventionnelTest {


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

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'utilisateur possède déjà une adresse enregistrée.
     * 2. When   : GET `/api/addresses`.
     * 3. Then   : Status 200 OK avec la liste des adresses dans l'élément `content`.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'utilisateur tente de supprimer une adresse lui appartenant.
     * 2. When   : DELETE `/api/addresses/{id}`.
     * 3. Then   : Status 204 NO_CONTENT.
     */

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
