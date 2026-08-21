package com.geolink.findme.service.adminService;

import com.geolink.findme.dto.response.AddressResponseDTO;
import com.geolink.findme.dto.response.UserProfileDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface du service d'administration pour la vue agrégée des utilisateurs et adresses.
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
}
