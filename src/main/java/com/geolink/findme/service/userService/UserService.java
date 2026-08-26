package com.geolink.findme.service.userService;

import com.geolink.findme.dto.request.UpdateProfileRequestDTO;
import com.geolink.findme.dto.response.UserProfileDTO;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * Téléverse ou remplace l'image de profil de l'utilisateur connecté.
     *
     * @param email l'adresse email de l'utilisateur
     * @param file  le fichier image envoyé
     * @return le DTO de profil mis à jour
     */
    UserProfileDTO uploadProfileImage(String email, MultipartFile file);
}
