package com.geolink.findme.service.passwordService;

import com.geolink.findme.entity.RefreshToken;
import com.geolink.findme.entity.User;

/**
 * Interface du service de gestion des refresh tokens.
 */
public interface RefreshTokenService {

    /**
     * Génère un nouveau refresh token pour un utilisateur et le persiste en base sous forme hashée.
     *
     * @param user l'utilisateur
     * @return le token brut (opaque UUID) à renvoyer au client
     */
    String createRefreshToken(User user);

    /**
     * Vérifie la validité d'un refresh token et effectue sa rotation (révoque le courant).
     *
     * @param rawRefreshToken le token brut fourni par le client
     * @return l'entité {@link RefreshToken} révoquée (contenant l'utilisateur)
     */
    RefreshToken verifyAndRotate(String rawRefreshToken);

    /**
     * Révoque tous les refresh tokens d'un utilisateur.
     *
     * @param user l'utilisateur
     */
    void revokeAllForUser(User user);

    /**
     * Révoque un refresh token spécifique.
     *
     * @param rawRefreshToken le token brut fourni par le client
     */
    void revokeToken(String rawRefreshToken);
}
