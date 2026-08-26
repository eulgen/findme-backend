package com.geolink.findme.repository;

import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.Address;
import com.geolink.findme.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration JPA du repository {@link AddressRepository}.
 * Vérifie les opérations de comptage par utilisateur, recherche par id/user et filtres admin.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testaddrdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Tests d'intégration JPA - AddressRepository")
class AddressRepositoryJpaTest {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Address address1;
    private Address address2;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        userRepository.deleteAll();

        user = User.builder()
                .email("owner@geolink.com")
                .fullName("Address Owner")
                .passwordHash("hashed")
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .build();

        address1 = Address.builder()
                .addressCode("ADR-2026-AAAA")
                .country("France")
                .city("Paris")
                .district("Le Marais")
                .street("Rue de Rivoli")
                .houseNumber("12")
                .build();

        address2 = Address.builder()
                .addressCode("ADR-2026-BBBB")
                .country("France")
                .city("Lyon")
                .district("Presqu'île")
                .street("Place Bellecour")
                .houseNumber("5")
                .build();

        addressRepository.save(address1);
        addressRepository.save(address2);

        // User est le côté propriétaire (Owning Side) de la relation @ManyToMany user_addresses
        user.getAddresses().add(address1);
        user.getAddresses().add(address2);
        userRepository.save(user);
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Un utilisateur possédant 2 adresses associées en BDD.
     * 2. When   : Appel de `countByUsers_Id(user.getId())`.
     * 3. Then   : Doit retourner 2.
     */
    @Test
    @DisplayName("Devrait compter exactement le nombre d'adresses rattachées à un utilisateur")
    void devrait_compter_nombre_adresses_par_utilisateur() {
        long count = addressRepository.countByUsers_Id(user.getId());

        assertThat(count).isEqualTo(2L);
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'adresse `address1` enregistrée pour l'utilisateur.
     * 2. When   : Recherche par ID de l'adresse et ID de l'utilisateur.
     * 3. Then   : Doit retourner l'adresse si elle appartient bien à l'utilisateur, et Optional.empty() pour un autre ID.
     */
    @Test
    @DisplayName("Devrait trouver une adresse par son ID et l'ID du propriétaire")
    void devrait_trouver_adresse_par_id_et_user_id() {
        Optional<Address> found = addressRepository.findByIdAndUserId(address1.getId(), user.getId());
        Optional<Address> notFound = addressRepository.findByIdAndUserId(address1.getId(), 999L);

        assertThat(found).isPresent();
        assertThat(found.get().getAddressCode()).isEqualTo("ADR-2026-AAAA");
        assertThat(notFound).isEmpty();
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Plusieurs adresses dans différentes villes ("Paris", "Lyon").
     * 2. When   : Recherche paginée filtrée par pays="France" et ville="Paris".
     * 3. Then   : La recherche admin doit renvoyer uniquement l'adresse de Paris.
     */
    @Test
    @DisplayName("Devrait rechercher les adresses avec filtres admin (pays, ville, quartier)")
    void devrait_rechercher_adresses_admin_avec_filtres() {
        Page<Address> page = addressRepository.searchAddresses("France", "Paris", null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getCity()).isEqualTo("Paris");
    }
}
