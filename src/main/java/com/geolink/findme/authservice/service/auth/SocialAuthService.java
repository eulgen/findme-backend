package com.geolink.findme.authservice.service.auth;

import com.geolink.findme.authservice.dto.request.AppleLoginRequestDTO;
import com.geolink.findme.authservice.dto.request.GoogleLoginRequestDTO;
import com.geolink.findme.authservice.dto.response.AuthResponseDTO;

/**
 * Interface de service dédiée à l'authentification sociale (Google & Apple Sign-In).
 * Respecte l'Inversion de Dépendance (DIP) et le Principe Ouvert/Fermé (OCP).
 */
public interface SocialAuthService {

    /**
     * Authentifie un utilisateur via son ID Token Google.
     *
     * @param dto la requête contenant le token d'identité Google
     * @return la réponse d'authentification contenant l'Access Token et le Refresh Token
     */
    AuthResponseDTO authenticateWithGoogle(GoogleLoginRequestDTO dto);

    /**
     * Authentifie un utilisateur via son ID Token Apple.
     *
     * @param dto la requête contenant le token d'identité Apple et les informations de profil optionnelles
     * @return la réponse d'authentification contenant l'Access Token et le Refresh Token
     */
    AuthResponseDTO authenticateWithApple(AppleLoginRequestDTO dto);
}
