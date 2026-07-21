package com.example.findme.dto.response;

import lombok.Builder;

/**
 * DTO (Data Transfer Object) renvoye en cas d'authentification reussie
 * (inscription ou connexion).
 *
 * <p>Contient le token JWT Bearer pour les futurs appels API,
 * ainsi que les informations de base de l'utilisateur connecte.</p>
 *
 * @param token le token JWT (Bearer)
 * @param user les informations publiques de l'utilisateur
 */
@Builder
public record AuthResponse(
        String token,
        UserResponse user
) {
}
