package com.geolink.findme.unit.non_conventionnel;

import com.geolink.findme.dto.mapper.AddressMapper;
import com.geolink.findme.dto.request.AddressRequestDTO;
import com.geolink.findme.entity.Address;
import com.geolink.findme.entity.GpsCoordinate;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.AddressNotFoundException;
import com.geolink.findme.exception.ForbiddenAccessException;
import com.geolink.findme.exception.MaxAddressLimitExceededException;
import com.geolink.findme.repository.AddressRepository;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.service.addressService.AddressServiceImpl;
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

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires AddressServiceImpl (Cas Non Conventionnels)")
class AddressServiceImplNonConventionnelTest {

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

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@geolink.com");
        testUser.setFullName("Jean Dupont");
        testUser.setAddresses(new HashSet<>());

        testAddress = new Address();
        testAddress.setId(10L);
        testAddress.setAddressCode("ADR-2026-X1Y2");
        testAddress.setUsers(new HashSet<>());
        testAddress.getUsers().add(testUser);
    }

    @Test
    @DisplayName("Devrait lever MaxAddressLimitExceededException si l'utilisateur a déjà 4 adresses lors de la création")
    void devrait_lever_une_exception_si_limite_maximale_d_adresses_atteinte() {
        when(addressRepository.countByUsers_Id(1L)).thenReturn(4L);

        assertThatThrownBy(() -> addressService.createAddress(testUser, new AddressRequestDTO()))
                .isInstanceOf(MaxAddressLimitExceededException.class)
                .hasMessageContaining("limite maximale est fixée à 4 adresses");

        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("Devrait lever MaxAddressLimitExceededException si l'utilisateur a déjà 4 adresses lors du raccordement")
    void devrait_lever_exception_si_limite_adresses_atteinte_lors_du_raccordement() {
        when(addressRepository.countByUsers_Id(1L)).thenReturn(4L);

        assertThatThrownBy(() -> addressService.linkAddressToUser(testUser, "ADR-2026-TEST"))
                .isInstanceOf(MaxAddressLimitExceededException.class)
                .hasMessageContaining("limite maximale est fixée à 4 adresses");
    }

    @Test
    @DisplayName("Devrait lever AddressNotFoundException si le code d'adresse à raccorder est introuvable")
    void devrait_lever_exception_si_raccordement_code_adresse_inexistant() {
        when(addressRepository.countByUsers_Id(1L)).thenReturn(2L);
        when(addressRepository.findByAddressCodeIgnoreCase("ADR-NOT-FOUND")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.linkAddressToUser(testUser, "ADR-NOT-FOUND"))
                .isInstanceOf(AddressNotFoundException.class)
                .hasMessageContaining("ADR-NOT-FOUND");
    }

    @Test
    @DisplayName("Devrait lever AddressNotFoundException si l'adresse est introuvable par son ID")
    void devrait_lever_exception_si_adresse_inexistante() {
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.getAddressById(testUser, 99L))
                .isInstanceOf(AddressNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Devrait lever ForbiddenAccessException si l'adresse appartient à un autre utilisateur")
    void devrait_lever_exception_si_adresse_appartient_a_un_autre_utilisateur() {
        User otherUser = new User();
        otherUser.setId(2L);

        Address otherAddress = new Address();
        otherAddress.setId(20L);
        otherAddress.setUsers(new HashSet<>());
        otherAddress.getUsers().add(otherUser);

        when(addressRepository.findById(20L)).thenReturn(Optional.of(otherAddress));

        assertThatThrownBy(() -> addressService.getAddressById(testUser, 20L))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessageContaining("Accès refusé");
    }

    @Test
    @DisplayName("Devrait lever AddressNotFoundException lorsque le code d'adresse recherché n'existe pas")
    void getAddressByCode_DevraitLeverException_QuandCodeInexistant() {
        when(addressRepository.findByAddressCodeIgnoreCase("UNKNOWN_CODE"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.getAddressByCode("UNKNOWN_CODE"))
                .isInstanceOf(AddressNotFoundException.class)
                .hasMessageContaining("UNKNOWN_CODE");
    }

    @Test
    @DisplayName("Devrait lever ForbiddenAccessException si un non-propriétaire tente d'exporter les données PDF")
    void devrait_lever_exception_si_export_pdf_par_non_proprietaire() {
        User otherUser = new User();
        otherUser.setId(2L);

        Address otherAddress = new Address();
        otherAddress.setId(20L);
        otherAddress.setUsers(new HashSet<>());
        otherAddress.getUsers().add(otherUser);

        when(addressRepository.findById(20L)).thenReturn(Optional.of(otherAddress));

        assertThatThrownBy(() -> addressService.exportAddressPdfData(testUser, 20L))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    @DisplayName("Devrait lever ForbiddenAccessException si un non-propriétaire tente d'uploader une photo")
    void devrait_lever_exception_si_upload_photo_par_non_proprietaire() {
        User otherUser = new User();
        otherUser.setId(2L);

        Address otherAddress = new Address();
        otherAddress.setId(20L);
        otherAddress.setUsers(new HashSet<>());
        otherAddress.getUsers().add(otherUser);

        when(addressRepository.findById(20L)).thenReturn(Optional.of(otherAddress));

        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "data".getBytes());

        assertThatThrownBy(() -> addressService.uploadPhoto(testUser, 20L, file))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    @DisplayName("Devrait gérer la mise à jour avec des coordonnées GPS partielles sans lever de NullPointerException")
    void devrait_gerer_coordonnees_gps_partielles_lors_de_la_mise_a_jour() {
        Address addrWithGps = new Address();
        addrWithGps.setId(10L);
        addrWithGps.setUsers(new HashSet<>());
        addrWithGps.getUsers().add(testUser);
        addrWithGps.setGpsCoordinate(new GpsCoordinate());

        when(addressRepository.findById(10L)).thenReturn(Optional.of(addrWithGps));
        when(addressRepository.save(any(Address.class))).thenReturn(addrWithGps);

        AddressRequestDTO dto = new AddressRequestDTO();
        com.geolink.findme.dto.response.GpsCoordinateDTO gpsDto = new com.geolink.findme.dto.response.GpsCoordinateDTO();
        gpsDto.setLatitude(48.8566);
        // longitude laissée volontairement nulle
        dto.setGps(gpsDto);

        addressService.updateAddress(testUser, 10L, dto);

        verify(addressRepository, times(1)).save(addrWithGps);
    }
}
