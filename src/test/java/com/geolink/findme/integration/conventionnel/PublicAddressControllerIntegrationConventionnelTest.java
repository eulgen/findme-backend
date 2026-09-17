package com.geolink.findme.integration.conventionnel;

import com.geolink.findme.entity.Address;
import com.geolink.findme.entity.AddressStatus;
import com.geolink.findme.entity.GpsCoordinate;
import com.geolink.findme.repository.AddressRepository;
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
 * Tests d'intégration Web conventionnels (cas nominaux) du contrôleur {@link PublicAddressController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testpublicaddrdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "management.health.mail.enabled=false"
})
@DisplayName("Tests d'intégration Web REST - PublicAddressController (Cas Conventionnels)")
class PublicAddressControllerIntegrationConventionnelTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AddressRepository addressRepository;

    private Address testAddress;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();

        testAddress = Address.builder()
                .addressCode("ADR-2026-PUBLIC")
                .country("Cameroun")
                .city("Yaoundé")
                .district("Bastos")
                .postalCode("00237")
                .street("Avenue des Ambassades")
                .houseNumber("123")
                .status(AddressStatus.VALIDE)
                .gpsCoordinate(GpsCoordinate.builder().latitude(3.8480).longitude(11.5021).build())
                .build();

        testAddress = addressRepository.save(testAddress);
    }

    @Test
    @DisplayName("GET /api/public/addresses/{addressCode} - Doit retourner l'adresse sans authentification")
    void getAddressByCode_PublicAccess_Success() throws Exception {
        mockMvc.perform(get("/api/public/addresses/ADR-2026-PUBLIC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressCode").value("ADR-2026-PUBLIC"))
                .andExpect(jsonPath("$.country").value("Cameroun"))
                .andExpect(jsonPath("$.city").value("Yaoundé"))
                .andExpect(jsonPath("$.district").value("Bastos"))
                .andExpect(jsonPath("$.postalCode").value("00237"))
                .andExpect(jsonPath("$.street").value("Avenue des Ambassades"))
                .andExpect(jsonPath("$.houseNumber").value("123"))
                .andExpect(jsonPath("$.status").value("VALIDE"))
                .andExpect(jsonPath("$.gps.latitude").value(3.8480))
                .andExpect(jsonPath("$.gps.longitude").value(11.5021));
    }

    @Test
    @DisplayName("GET /api/public/addresses/{addressCode} - Doit fonctionner de manière insensible à la casse")
    void getAddressByCode_CaseInsensitive_Success() throws Exception {
        mockMvc.perform(get("/api/public/addresses/adr-2026-public")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressCode").value("ADR-2026-PUBLIC"))
                .andExpect(jsonPath("$.city").value("Yaoundé"));
    }

    @Test
    @DisplayName("POST /api/public/addresses - Doit créer une adresse de manière anonyme sans authentification")
    void createPublicAddress_Success() throws Exception {
        String jsonPayload = """
                {
                    "country": "Cameroun",
                    "city": "Douala",
                    "district": "Akwa",
                    "street": "Rue Joffre",
                    "houseNumber": "45",
                    "photoUrl": "https://cdn.findme.com/photos/douala.jpg",
                    "gps": {
                        "latitude": 4.0511,
                        "longitude": 9.7085
                    }
                }
                """;

        mockMvc.perform(post("/api/public/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addressCode").isNotEmpty())
                .andExpect(jsonPath("$.city").value("Douala"))
                .andExpect(jsonPath("$.district").value("Akwa"));
    }
}
