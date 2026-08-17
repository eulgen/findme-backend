package com.geolink.findme.authservice.service.userService;

import com.geolink.findme.authservice.dto.request.UpdateProfileRequestDTO;
import com.geolink.findme.authservice.dto.response.UserProfileDTO;

/**
 * Interface du service de gestion des utilisateurs et profils.
 */
public interface UserService {

    /**
     * Récupère le profil de l'utilisateur identifié par son email.
     *
     * @param email l'adresse email
     * @return le DTO de profil
     */
    UserProfileDTO getProfile(String email);

    /**
     * Met à jour le profil (prénom, nom) de l'utilisateur identifié par son email.
     *
     * @param email l'adresse email
     * @param dto   les nouvelles informations
     * @return le DTO de profil mis à jour
     */
    UserProfileDTO updateProfile(String email, UpdateProfileRequestDTO dto);
}
