package com.example.findme.service;

import com.example.findme.dto.request.SigninRequest;
import com.example.findme.dto.request.SignupRequest;
import com.example.findme.dto.response.AuthResponse;

/**
 * Interface decrivant les cas d'utilisation (use cases) lies a l'authentification.
 * Respecte le principe de segrégation des interfaces (I de SOLID).
 *
 * @author findme-team
 */
public interface AuthService {

    /**
     * Inscrit un nouvel utilisateur dans le systeme.
     *
     * @param request les informations d'inscription validees
     * @return les informations de l'utilisateur avec son token
     * @throws com.example.findme.exception.DuplicateResourceException si l'email ou le username existe deja
     */
    AuthResponse signup(SignupRequest request);

    /**
     * Authentifie un utilisateur existant.
     *
     * @param request les identifiants de connexion
     * @return les informations de l'utilisateur avec son token
     * @throws com.example.findme.exception.ResourceNotFoundException si les identifiants sont incorrects
     */
    AuthResponse signin(SigninRequest request);
}
