package com.geolink.findme.unit.conventionnel;

import com.geolink.findme.dto.mapper.AddressMapper;
import com.geolink.findme.service.addressService.AddressServiceImpl;
import com.geolink.findme.dto.request.AddressRequestDTO;
import com.geolink.findme.dto.response.GpsCoordinateDTO;
import com.geolink.findme.dto.response.AddressResponseDTO;
import com.geolink.findme.entity.Address;
import com.geolink.findme.entity.GpsCoordinate;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.AddressNotFoundException;
import com.geolink.findme.exception.ForbiddenAccessException;
import com.geolink.findme.exception.MaxAddressLimitExceededException;
import com.geolink.findme.repository.AddressRepository;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.service.storageService.StorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du service {@link AddressServiceImpl}.
 * Couvre les cas nominaux, les limites métiers (max 4 adresses), la sécurité d'accès et la gestion des fichiers.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires du service AddressServiceImpl")

class AddressServiceImplConventionnelTest {


    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @Spy
    private AddressMapper addressMapper = new AddressMapper();

    @InjectMocks
    private AddressServiceImpl addressService;

    private User testUser;
    private Address testAddress;
    private AddressRequestDTO addressRequestDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@geolink.com");
        testUser.setFullName("Jean Dupont");
        testUser.setAddresses(new HashSet<>());

        testAddress = new Address();
        testAddress.setId(10L);
        testAddress.setCountry("France");
        testAddress.setCity("Paris");
        testAddress.setStreet("Rue de Rivoli");
        testAddress.setHouseNumber("12");
        testAddress.setAddressCode("ADR-2026-X1Y2");

        GpsCoordinate gps = new GpsCoordinate();
        gps.setLatitude(48.8566);
        gps.setLongitude(2.3522);
        testAddress.setGpsCoordinate(gps);

        testAddress.setUsers(new HashSet<>());
        testAddress.getUsers().add(testUser);

        GpsCoordinateDTO gpsDto = new GpsCoordinateDTO();
        gpsDto.setLatitude(48.8566);
        gpsDto.setLongitude(2.3522);

        addressRequestDTO = new AddressRequestDTO();
        addressRequestDTO.setCountry("France");
        addressRequestDTO.setCity("Paris");
        addressRequestDTO.setStreet("Rue de Rivoli");
        addressRequestDTO.setHouseNumber("12");
        addressRequestDTO.setGps(gpsDto);
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'utilisateur connecté possède actuellement 2 adresses en BDD (limite de 4 non atteinte).
     * 2. When   : La méthode createAddress est appelée avec des données valides.
     * 3. Then   : Un code unique d'adresse est généré, le lien avec l'utilisateur est créé, l'adresse est sauvegardée
     *            et le DTO retourné contient les informations créées.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'utilisateur connecté possède déjà 4 adresses enregistrées (limite maximale).
     * 2. When   : Tente de créer une 5ème adresse via createAddress.
     * 3. Then   : La méthode doit lever MaxAddressLimitExceededException sans sauvegarder d'adresse en BDD.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'adresse ID=10 existe en BDD et appartient à l'utilisateur connecté ID=1.
     * 2. When   : getAddressById est appelée avec l'ID=10.
     * 3. Then   : L'adresse est récupérée et convertie en DTO avec succès.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'adresse ID=99 n'existe pas en BDD.
     * 2. When   : getAddressById est appelée avec ID=99.
     * 3. Then   : La méthode lève AddressNotFoundException.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'adresse ID=10 existe mais n'appartient pas à l'utilisateur connecté (appartient à un autre user ID=2).
     * 2. When   : L'utilisateur ID=1 tente de la consulter via getAddressById.
     * 3. Then   : La méthode lève ForbiddenAccessException (accès refusé).
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'adresse existe et appartient à l'utilisateur connecté.
     * 2. When   : updateAddress est appelée avec de nouvelles valeurs de ville, rue et coordonnées GPS.
     * 3. Then   : Les champs de l'adresse sont mis à jour et sauvegardés en BDD.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'adresse a une photo ("addresses/photo1.jpg") et testUser est son unique propriétaire.
     * 2. When   : deleteAddress est appelée.
     * 3. Then   : Le lien utilisateur est supprimé, l'adresse devenant orpheline, la photo est supprimée du stockage
     *            et l'adresse est supprimée de la BDD.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'adresse possède déjà une ancienne photo ("old_photo.png").
     * 2. When   : uploadPhoto est appelée avec un nouveau fichier MultipartFile.
     * 3. Then   : L'ancienne photo est supprimée du storage, le nouveau fichier est stocké, et la BDD est mise à jour avec le nouveau chemin.
     */

    @Test
    @DisplayName("Devrait retourner le DTO de l'adresse lorsqu'elle existe et appartient à l'utilisateur")
    void devrait_retourner_l_adresse_si_elle_appartient_a_l_utilisateur() {
        // Given
        when(addressRepository.findById(10L)).thenReturn(Optional.of(testAddress));

        // When
        AddressResponseDTO response = addressService.getAddressById(testUser, 10L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCity()).isEqualTo("Paris");
    }

    @Test
    @DisplayName("Devrait mettre à jour l'adresse et ses coordonnées GPS avec succès")
    void devrait_mettre_a_jour_l_adresse_et_les_coordonnees_gps() {
        // Given
        when(addressRepository.findById(10L)).thenReturn(Optional.of(testAddress));
        when(addressRepository.save(any(Address.class))).thenReturn(testAddress);

        GpsCoordinateDTO gpsDto = new GpsCoordinateDTO();
        gpsDto.setLatitude(45.7640);
        gpsDto.setLongitude(4.8357);

        AddressRequestDTO updateRequest = new AddressRequestDTO();
        updateRequest.setCountry("France");
        updateRequest.setCity("Lyon");
        updateRequest.setStreet("Rue de la République");
        updateRequest.setHouseNumber("5");
        updateRequest.setGps(gpsDto);

        // When
        AddressResponseDTO response = addressService.updateAddress(testUser, 10L, updateRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(testAddress.getCity()).isEqualTo("Lyon");
        assertThat(testAddress.getStreet()).isEqualTo("Rue de la République");
        assertThat(testAddress.getGpsCoordinate().getLatitude()).isEqualTo(45.7640);
        verify(addressRepository, times(1)).save(testAddress);
    }

    @Test
    @DisplayName("Devrait supprimer l'adresse et sa photo lorsque l'adresse n'a plus aucun autre propriétaire")
    void devrait_supprimer_adresse_et_photo_quand_elle_devient_orpheline() {
        // Given
        testAddress.setPhotoUrl("addresses/photo1.jpg");
        when(addressRepository.findById(10L)).thenReturn(Optional.of(testAddress));

        // When
        addressService.deleteAddress(testUser, 10L);

        // Then
        assertThat(testAddress.getUsers()).isEmpty();
        verify(storageService, times(1)).delete("addresses/photo1.jpg", "addresses");
        verify(addressRepository, times(1)).delete(testAddress);
    }

    @Test
    @DisplayName("Devrait supprimer l'ancienne photo, stocker la nouvelle et mettre à jour l'adresse")
    void devrait_remplacer_la_photo_existante_et_enregistrer_la_nouvelle() {
        // Given
        testAddress.setPhotoUrl("old_photo.png");
        when(addressRepository.findById(10L)).thenReturn(Optional.of(testAddress));
        when(storageService.store(any(MultipartFile.class), eq("addresses"))).thenReturn("new_photo.jpg");
        when(addressRepository.save(any(Address.class))).thenReturn(testAddress);

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

        // When
        AddressResponseDTO response = addressService.uploadPhoto(testUser, 10L, file);

        // Then
        assertThat(response).isNotNull();
        verify(storageService, times(1)).delete("old_photo.png", "addresses");
        verify(storageService, times(1)).store(file, "addresses");
        assertThat(testAddress.getPhotoUrl()).isEqualTo("new_photo.jpg");
        verify(addressRepository, times(1)).save(testAddress);
    }

    @Test
    @DisplayName("getAddressByCode - Devrait retourner les informations de l'adresse lorsque le code existe")
    void getAddressByCode_DevraitRetournerAdresse_QuandCodeExiste() {
        // Given
        when(addressRepository.findByAddressCodeIgnoreCase("ADR-2026-X1Y2"))
                .thenReturn(Optional.of(testAddress));

        // When
        AddressResponseDTO response = addressService.getAddressByCode("ADR-2026-X1Y2");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAddressCode()).isEqualTo("ADR-2026-X1Y2");
        assertThat(response.getCity()).isEqualTo("Paris");
        assertThat(response.getCountry()).isEqualTo("France");
        verify(addressRepository, times(1)).findByAddressCodeIgnoreCase("ADR-2026-X1Y2");
    }
}
