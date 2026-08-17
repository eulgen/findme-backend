package com.geolink.findme.authservice.service.auth;

import com.geolink.findme.authservice.dto.request.RefreshRequestDTO;
import com.geolink.findme.authservice.dto.request.SignInRequestDTO;
import com.geolink.findme.authservice.dto.request.SignUpRequestDTO;
import com.geolink.findme.authservice.dto.response.AuthResponseDTO;
import com.geolink.findme.authservice.dto.response.UserProfileDTO;

/**
 * Interface du service d'authentification principal.
 */
public interface AuthService {

    /**
     * Inscription d'un nouvel utilisateur.
     *
     * @param dto les données d'inscription
     * @return le profil utilisateur créé
     */
    UserProfileDTO signUp(SignUpRequestDTO dto);

    /**
     * Connexion d'un utilisateur existant.
     *
     * @param dto les identifiants
     * @return les tokens d'accès et de rafraîchissement
     */
    AuthResponseDTO signIn(SignInRequestDTO dto);

    /**
     * Rafraîchissement des tokens.
     *
     * @param dto la requête contenant le refresh token
     * @return la nouvelle paire de tokens
     */
    AuthResponseDTO refresh(RefreshRequestDTO dto);

    /**
     * Déconnexion d'un utilisateur.
     *
     * @param email l'email de l'utilisateur connecté
     */
    void logout(String email);
}
