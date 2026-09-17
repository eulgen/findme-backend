package com.geolink.findme.unit.conventionnel;

import com.geolink.findme.dto.mapper.AddressMapper;
import com.geolink.findme.service.adminService.AdminServiceImpl;
import com.geolink.findme.dto.mapper.UserMapper;
import com.geolink.findme.dto.response.AddressResponseDTO;
import com.geolink.findme.dto.response.UserProfileDTO;
import com.geolink.findme.entity.Address;
import com.geolink.findme.entity.Role;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.RoleNotFoundException;
import com.geolink.findme.exception.UserNotFoundException;
import com.geolink.findme.repository.AddressRepository;
import com.geolink.findme.repository.RoleRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du service d'administration {@link AdminServiceImpl}.
 * Vérifie la recherche paginée des utilisateurs/adresses et le changement de rôle d'administration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires du service AdminServiceImpl")

class AdminServiceImplConventionnelTest {


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
    private Role adminRole;
    private Address testAddress;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("user@geolink.com");
        testUser.setFullName("User Test");
        testUser.setRoles(new HashSet<>());

        adminRole = Role.builder().id(2L).name("ADMIN").build();
        testAddress = Address.builder().id(100L).city("Paris").country("France").build();
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Le repository userRepository renvoie une page d'utilisateurs contenant testUser lors d'une recherche textuelle.
     * 2. When   : getAllUsers est appelée avec le mot-clé de recherche et une pagination.
     * 3. Then   : La page de UserProfileDTO retournée contient les informations des utilisateurs converties.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Le repository addressRepository renvoie une page d'adresses filtrées par pays/ville/quartier.
     * 2. When   : getAllAddresses est appelée avec les critères de filtrage.
     * 3. Then   : La page d'adresses retournée contient les DTOs correctement assemblés.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'utilisateur ID=10 existe et le rôle "ADMIN" est présent en BDD.
     * 2. When   : updateUserRole(10L, "ADMIN") est exécutée.
     * 3. Then   : Les rôles de l'utilisateur sont réinitialisés avec le nouveau rôle ADMIN et l'utilisateur est sauvegardé en BDD.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'utilisateur ID=99 n'existe pas en BDD.
     * 2. When   : updateUserRole(99L, "ADMIN") est exécutée.
     * 3. Then   : La méthode lève UserNotFoundException sans modifier aucun rôle.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'utilisateur ID=10 existe mais le nom de rôle "SUPER_HERO" n'existe pas en BDD.
     * 2. When   : updateUserRole(10L, "SUPER_HERO") est appelée.
     * 3. Then   : La méthode lève RoleNotFoundException sans sauvegarder de modifications.
     */

    @Test
    @DisplayName("Devrait retourner la page des profils utilisateurs filtrée")
    void devrait_retourner_la_liste_paginee_des_utilisateurs_filtree() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> usersPage = new PageImpl<>(List.of(testUser));
        when(userRepository.searchUsers("User", pageable)).thenReturn(usersPage);

        // When
        Page<UserProfileDTO> result = adminService.getAllUsers("User", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("user@geolink.com");
        verify(userRepository, times(1)).searchUsers("User", pageable);
    }

    @Test
    @DisplayName("Devrait retourner la page des adresses filtrée par localisation")
    void devrait_retourner_la_liste_paginee_des_adresses_filtree() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Address> addressPage = new PageImpl<>(List.of(testAddress));
        when(addressRepository.searchAddresses("France", "Paris", null, pageable)).thenReturn(addressPage);

        // When
        Page<AddressResponseDTO> result = adminService.getAllAddresses("France", "Paris", null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCity()).isEqualTo("Paris");
        verify(addressRepository, times(1)).searchAddresses("France", "Paris", null, pageable);
    }

    @Test
    @DisplayName("Devrait mettre à jour le rôle d'un utilisateur avec succès")
    void devrait_mettre_a_jour_le_role_de_l_utilisateur_avec_succes() {
        // Given
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

        // When
        adminService.updateUserRole(10L, "ADMIN");

        // Then
        assertThat(testUser.getRoles()).containsExactly(adminRole);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Devrait mettre à jour le statut d'une adresse avec succès")
    void devrait_mettre_a_jour_le_statut_d_une_adresse() {
        // Given
        when(addressRepository.findById(100L)).thenReturn(Optional.of(testAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AddressResponseDTO response = adminService.updateAddressStatus(100L, com.geolink.findme.entity.AddressStatus.VALIDE);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(com.geolink.findme.entity.AddressStatus.VALIDE);
        verify(addressRepository, times(1)).save(testAddress);
    }

    @Test
    @DisplayName("Devrait supprimer une adresse et nettoyer le fichier média associé")
    void devrait_supprimer_une_adresse_et_son_media() {
        // Given
        testAddress.setPhotoUrl("photo.jpg");
        testAddress.getUsers().add(testUser);
        testUser.getAddresses().add(testAddress);
        when(addressRepository.findById(100L)).thenReturn(Optional.of(testAddress));

        // When
        adminService.deleteAddress(100L);

        // Then
        verify(storageService, times(1)).delete("photo.jpg", "addresses");
        verify(addressRepository, times(1)).delete(testAddress);
    }

    @Test
    @DisplayName("Devrait retourner la liste des adresses d'un utilisateur par son ID")
    void devrait_retourner_les_adresses_d_un_utilisateur() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Address> addressPage = new PageImpl<>(List.of(testAddress));
        when(userRepository.existsById(10L)).thenReturn(true);
        when(addressRepository.findByUserId(10L, pageable)).thenReturn(addressPage);

        // When
        Page<AddressResponseDTO> result = adminService.getAddressesByUserId(10L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(addressRepository, times(1)).findByUserId(10L, pageable);
    }

    @Test
    @DisplayName("Devrait retourner les utilisateurs propriétaires d'une adresse par son ID")
    void devrait_retourner_les_utilisateurs_d_une_adresse() {
        // Given
        when(addressRepository.existsById(100L)).thenReturn(true);
        when(userRepository.findByAddresses_Id(100L)).thenReturn(List.of(testUser));

        // When
        List<UserProfileDTO> result = adminService.getUsersByAddressId(100L);

        // Then
        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("user@geolink.com");
        verify(userRepository, times(1)).findByAddresses_Id(100L);
    }
}
