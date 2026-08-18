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
     * Déconnexion d'un utilisateur par email.
     *
     * @param email l'email de l'utilisateur connecté
     */
    void logout(String email);

    /**
     * Déconnexion et révocation d'un refresh token spécifique.
     *
     * @param refreshToken le jeton de rafraîchissement à révoquer
     */
    void logoutWithToken(String refreshToken);

    /**
     * Vérification de compte par code OTP.
     *
     * @param email l'email de l'utilisateur
     * @param code le code OTP reçu par mail
     */
    void verifyAccount(String email, String code);

    /**
     * Renvoi d'un nouveau code OTP de vérification de compte.
     *
     * @param email l'email de l'utilisateur
     */
    void resendVerificationOtp(String email);
}
