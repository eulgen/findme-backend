package com.geolink.findme.service.adminService;

import com.geolink.findme.dto.mapper.AddressMapper;
import com.geolink.findme.dto.mapper.UserMapper;
import com.geolink.findme.dto.response.AddressResponseDTO;
import com.geolink.findme.dto.response.UserProfileDTO;
import com.geolink.findme.entity.Address;
import com.geolink.findme.entity.User;
import com.geolink.findme.entity.Role;
import com.geolink.findme.exception.RoleNotFoundException;
import com.geolink.findme.exception.UserNotFoundException;
import com.geolink.findme.repository.AddressRepository;
import com.geolink.findme.repository.RoleRepository;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.service.storageService.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service d'administration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AddressRepository addressRepository;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final StorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileDTO> getAllUsers(String search, Pageable pageable) {
        log.info("Administration : Consultation de la liste des utilisateurs (filtre de recherche = '{}')", search);
        Page<User> usersPage = userRepository.searchUsers(search, pageable);
        return usersPage.map(userMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AddressResponseDTO> getAllAddresses(String country, String city, String district, Pageable pageable) {
        log.info("Administration : Consultation de la liste des adresses (pays='{}', ville='{}', quartier='{}')", country, city, district);
        Page<Address> addressesPage = addressRepository.searchAddresses(country, city, district, pageable);
        return addressesPage.map(address -> addressMapper.toDTO(address, storageService));
    }

    @Override
    @Transactional
    public void updateUserRole(Long userId, String roleName) {
        log.info("Administration : Mise à jour du rôle de l'utilisateur ID={} avec le rôle {}", userId, roleName);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable avec l'ID: " + userId));

        Role newRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RoleNotFoundException("Rôle introuvable : " + roleName));

        user.getRoles().clear();
        user.getRoles().add(newRole);
        userRepository.save(user);
    }
}
