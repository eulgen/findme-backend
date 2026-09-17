package com.geolink.findme.integration.non_conventionnel;

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
 * Tests d'intégration non conventionnels du contrôleur {@link AddressController}.
 * Couvre les cas d'erreurs : limite dépassée, accès interdit, adresse inexistante, sans authentification.
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
@DisplayName("Tests d'intégration Web REST - AddressController (Non Conventionnels)")
class AddressControllerIntegrationNonConventionnelTest {

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
    private User otherUser;

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

        otherUser = User.builder()
                .email("other.user@geolink.com")
                .fullName("Other User")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();
        userRepository.saveAndFlush(otherUser);
    }

    // ======================= Création =======================

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

    @Test
    @DisplayName("Devrait refuser la création d'une 5ème adresse (409 Conflict)")
    void devrait_refuser_creation_si_limite_4_adresses_atteinte() throws Exception {
        // Créer 4 adresses existantes liées à testUser
        for (int i = 1; i <= 4; i++) {
            Address addr = Address.builder()
                    .addressCode("ADR-2026-LIM" + i)
                    .country("France")
                    .city("Paris")
                    .district("Quartier " + i)
                    .street("Rue de la Paix")
                    .build();
            addr.getUsers().add(testUser);
            Address saved = addressRepository.save(addr);
            testUser.getAddresses().add(saved);
        }
        userRepository.saveAndFlush(testUser);

        UserPrincipal principal = new UserPrincipal(testUser);

        AddressRequestDTO request = new AddressRequestDTO();
        request.setCountry("France");
        request.setCity("Lyon");
        request.setDistrict("Presqu'île");
        request.setStreet("Rue de la République");
        request.setPhotoUrl("https://cdn.findme.com/photos/lyon.jpg");
        request.setGps(new GpsCoordinateDTO(45.764043, 4.835659));

        mockMvc.perform(post("/api/addresses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Devrait refuser la création d'adresse sans authentification (401 Unauthorized)")
    void devrait_refuser_creation_sans_authentification() throws Exception {
        AddressRequestDTO request = new AddressRequestDTO();
        request.setCountry("France");
        request.setCity("Paris");

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ======================= Consultation =======================

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

        mockMvc.perform(get("/api/addresses")
                        .with(user(new UserPrincipal(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Devrait refuser l'accès à une adresse appartenant à un autre utilisateur (403 Forbidden)")
    void devrait_refuser_acces_adresse_autre_utilisateur() throws Exception {
        Address otherAddr = Address.builder()
                .addressCode("ADR-2026-OTHER")
                .country("Belgique")
                .city("Bruxelles")
                .district("Ixelles")
                .build();
        otherAddr.getUsers().add(otherUser);
        Address saved = addressRepository.save(otherAddr);

        mockMvc.perform(get("/api/addresses/" + saved.getId())
                        .with(user(new UserPrincipal(testUser))))
                .andExpect(status().isForbidden());
    }

    // ======================= Suppression =======================

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

        mockMvc.perform(delete("/api/addresses/" + addr.getId())
                        .with(user(new UserPrincipal(testUser))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Devrait retourner 404 si l'adresse à supprimer est inexistante")
    void devrait_retourner_404_si_adresse_a_supprimer_inexistante() throws Exception {
        mockMvc.perform(delete("/api/addresses/99999")
                        .with(user(new UserPrincipal(testUser))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Devrait refuser la suppression d'une adresse appartenant à un autre utilisateur (403 Forbidden)")
    void devrait_refuser_suppression_adresse_autre_utilisateur() throws Exception {
        Address otherAddr = Address.builder()
                .addressCode("ADR-2026-OTH2")
                .country("Espagne")
                .city("Madrid")
                .district("Centro")
                .build();
        otherAddr.getUsers().add(otherUser);
        Address saved = addressRepository.save(otherAddr);

        mockMvc.perform(delete("/api/addresses/" + saved.getId())
                        .with(user(new UserPrincipal(testUser))))
                .andExpect(status().isForbidden());
    }

    // ======================= Raccordement =======================

    @Test
    @DisplayName("Devrait retourner 404 si le code d'adresse à raccorder est inexistant")
    void devrait_retourner_404_si_code_adresse_inexistant_lors_raccordement() throws Exception {
        mockMvc.perform(post("/api/addresses/link/ADR-NOT-FOUND")
                        .with(user(new UserPrincipal(testUser))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Devrait refuser le raccordement si l'utilisateur a déjà 4 adresses (409 Conflict)")
    void devrait_refuser_raccordement_si_limite_atteinte() throws Exception {
        for (int i = 1; i <= 4; i++) {
            Address addr = Address.builder()
                    .addressCode("ADR-2026-RC" + i)
                    .country("France")
                    .city("Nantes")
                    .district("Zone " + i)
                    .street("Rue des Carmes")
                    .build();
            addr.getUsers().add(testUser);
            Address saved = addressRepository.save(addr);
            testUser.getAddresses().add(saved);
        }
        userRepository.saveAndFlush(testUser);

        Address targetAddr = Address.builder()
                .addressCode("ADR-2026-TARGET")
                .country("France")
                .city("Strasbourg")
                .district("Centre")
                .build();
        addressRepository.save(targetAddr);

        mockMvc.perform(post("/api/addresses/link/ADR-2026-TARGET")
                        .with(user(new UserPrincipal(testUser))))
                .andExpect(status().isConflict());
    }

    // ======================= Modification & Upload & Export =======================

    @Test
    @DisplayName("Devrait refuser la mise à jour d'une adresse appartenant à un autre utilisateur (403 Forbidden)")
    void devrait_refuser_mise_a_jour_adresse_autre_utilisateur() throws Exception {
        Address otherAddr = Address.builder()
                .addressCode("ADR-2026-OTH3")
                .country("Espagne")
                .city("Barcelone")
                .district("Gràcia")
                .build();
        otherAddr.getUsers().add(otherUser);
        Address saved = addressRepository.save(otherAddr);

        AddressRequestDTO request = new AddressRequestDTO();
        request.setCountry("Espagne");
        request.setCity("Barcelone");
        request.setDistrict("Modifié");
        request.setStreet("Carrer Gran");
        request.setPhotoUrl("https://cdn.findme.com/photos/barcelona.jpg");
        request.setGps(new GpsCoordinateDTO(41.4036, 2.1744));

        mockMvc.perform(put("/api/addresses/" + saved.getId())
                        .with(user(new UserPrincipal(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Devrait retourner 404 si l'adresse à modifier n'existe pas")
    void devrait_retourner_404_si_adresse_a_modifier_inexistante() throws Exception {
        AddressRequestDTO request = new AddressRequestDTO();
        request.setCountry("France");
        request.setCity("Paris");
        request.setDistrict("Inexistant");
        request.setStreet("Rue Inconnue");
        request.setPhotoUrl("https://cdn.findme.com/photos/paris.jpg");
        request.setGps(new GpsCoordinateDTO(48.8566, 2.3522));

        mockMvc.perform(put("/api/addresses/99999")
                        .with(user(new UserPrincipal(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Devrait refuser l'upload de photo si le format de fichier est invalide (400 Bad Request)")
    void devrait_refuser_upload_photo_si_format_invalide() throws Exception {
        Address addr = Address.builder()
                .addressCode("ADR-2026-PHOTO")
                .country("France")
                .city("Lille")
                .district("Fives")
                .build();
        addr.getUsers().add(testUser);
        Address saved = addressRepository.save(addr);

        MockMultipartFile pdfFile = new MockMultipartFile(
                "photo",
                "document.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/addresses/" + saved.getId() + "/photo")
                        .file(pdfFile)
                        .with(user(new UserPrincipal(testUser))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait refuser l'export PDF d'une adresse d'un autre utilisateur (403 Forbidden)")
    void devrait_refuser_export_pdf_adresse_autre_utilisateur() throws Exception {
        Address otherAddr = Address.builder()
                .addressCode("ADR-2026-OTH4")
                .country("Italie")
                .city("Rome")
                .district("Trastevere")
                .build();
        otherAddr.getUsers().add(otherUser);
        Address saved = addressRepository.save(otherAddr);

        mockMvc.perform(get("/api/addresses/" + saved.getId() + "/export")
                        .with(user(new UserPrincipal(testUser))))
                .andExpect(status().isForbidden());
    }
}
