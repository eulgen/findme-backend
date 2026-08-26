package com.geolink.findme.service.adminService;

import com.geolink.findme.dto.response.AddressResponseDTO;
import com.geolink.findme.dto.response.UserProfileDTO;
import com.geolink.findme.entity.AddressStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface du service d'administration pour la vue agrégée et les actions d'administration des utilisateurs et adresses.
 */
public interface AdminService {

    /**
     * Récupère la liste paginée des utilisateurs avec option de recherche par nom/email.
     */
    Page<UserProfileDTO> getAllUsers(String search, Pageable pageable);

    /**
     * Récupère la liste paginée de toutes les adresses avec filtres par pays, ville, quartier.
     */
    Page<AddressResponseDTO> getAllAddresses(String country, String city, String district, Pageable pageable);

    /**
     * Met à jour le rôle d'un utilisateur.
     */
    void updateUserRole(Long userId, String roleName);

    /**
     * Met à jour le statut d'une adresse (EN_ATTENTE, VALIDE, NON_VALIDE).
     */
    AddressResponseDTO updateAddressStatus(Long addressId, AddressStatus status);

    /**
     * Supprime définitivement une adresse par son ID.
     */
    void deleteAddress(Long addressId);

    /**
     * Récupère la liste paginée de toutes les adresses créées par un utilisateur spécifique.
     */
    Page<AddressResponseDTO> getAddressesByUserId(Long userId, Pageable pageable);

    /**
     * Récupère la liste des utilisateurs associés (propriétaires) à une adresse donnée.
     */
    List<UserProfileDTO> getUsersByAddressId(Long addressId);
}
