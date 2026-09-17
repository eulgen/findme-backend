package com.geolink.findme.unit.non_conventionnel;

import com.geolink.findme.dto.mapper.AddressMapper;
import com.geolink.findme.dto.mapper.UserMapper;
import com.geolink.findme.entity.Address;
import com.geolink.findme.entity.AddressStatus;
import com.geolink.findme.entity.Role;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.AddressNotFoundException;
import com.geolink.findme.exception.RoleNotFoundException;
import com.geolink.findme.exception.UserNotFoundException;
import com.geolink.findme.repository.AddressRepository;
import com.geolink.findme.repository.RoleRepository;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.service.adminService.AdminServiceImpl;
import com.geolink.findme.service.storageService.StorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires AdminServiceImpl (Cas Non Conventionnels)")
class AdminServiceImplNonConventionnelTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private StorageService storageService;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @Spy
    private AddressMapper addressMapper = new AddressMapper();

    @InjectMocks
    private AdminServiceImpl adminService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("user@geolink.com");
        testUser.setFullName("User Test");
        testUser.setRoles(new HashSet<>());
    }

    @Test
    @DisplayName("Devrait lever UserNotFoundException si l'utilisateur est introuvable lors de la mise à jour du rôle")
    void devrait_lever_exception_si_utilisateur_introuvable_lors_du_changement_de_role() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUserRole(99L, "ADMIN"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Devrait lever RoleNotFoundException si le rôle spécifié est introuvable")
    void devrait_lever_exception_si_role_introuvable_lors_du_changement_de_role() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("SUPER_HERO")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUserRole(10L, "SUPER_HERO"))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("SUPER_HERO");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Devrait lever AddressNotFoundException lors du changement de statut d'une adresse inexistante")
    void devrait_lever_exception_si_changement_statut_adresse_inexistante() {
        when(addressRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateAddressStatus(999L, AddressStatus.VALIDE))
                .isInstanceOf(AddressNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Devrait lever AddressNotFoundException lors de la suppression d'une adresse inexistante par un admin")
    void devrait_lever_exception_si_suppression_adresse_inexistante() {
        when(addressRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteAddress(999L))
                .isInstanceOf(AddressNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Devrait lever UserNotFoundException lors du listage des adresses d'un utilisateur inexistant")
    void devrait_lever_exception_si_consultation_adresses_utilisateur_inexistant() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> adminService.getAddressesByUserId(999L, PageRequest.of(0, 10)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Devrait lever AddressNotFoundException lors de la recherche des propriétaires d'une adresse inexistante")
    void devrait_lever_exception_si_consultation_proprietaires_adresse_inexistante() {
        when(addressRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> adminService.getUsersByAddressId(999L))
                .isInstanceOf(AddressNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Devrait supprimer l'adresse en BDD même si la suppression de la photo échoue")
    void devrait_supprimer_adresse_meme_si_suppression_photo_echoue() {
        Address addrWithPhoto = Address.builder()
                .id(200L)
                .photoUrl("addresses/photo.jpg")
                .build();
        addrWithPhoto.setUsers(new java.util.HashSet<>());

        when(addressRepository.findById(200L)).thenReturn(Optional.of(addrWithPhoto));
        doThrow(new RuntimeException("Stockage indisponible"))
                .when(storageService).delete("addresses/photo.jpg", "addresses");

        // Le service capture l'exception storage et doit tout de même supprimer l'adresse
        adminService.deleteAddress(200L);

        verify(addressRepository, times(1)).delete(addrWithPhoto);
    }
}
