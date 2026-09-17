package com.geolink.findme.integration.non_conventionnel;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration Web non conventionnels (cas d'erreur) du contrôleur {@link PublicAddressController}.
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
@DisplayName("Tests d'intégration Web REST - PublicAddressController (Cas Non Conventionnels)")
class PublicAddressControllerIntegrationNonConventionnelTest {

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
    @DisplayName("GET /api/public/addresses/{addressCode} - Code inexistant doit retourner 404 Not Found")
    void getAddressByCode_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/public/addresses/ADR-NOT-FOUND")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Adresse non trouvée"));
    }
}
