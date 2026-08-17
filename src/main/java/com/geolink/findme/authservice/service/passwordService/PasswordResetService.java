package com.geolink.findme.authservice.service.passwordService;

/**
 * Interface du service de réinitialisation de mot de passe.
 */
public interface PasswordResetService {

    /**
     * Initie une demande de réinitialisation de mot de passe pour l'email donné.
     * Si le compte n'existe pas, la méthode se termine silencieusement (anti-account enumeration).
     *
     * @param email l'adresse email
     */
    void requestReset(String email);

    /**
     * Réinitialise le mot de passe à l'aide d'un token valide.
     *
     * @param token       le token de réinitialisation brut
     * @param newPassword le nouveau mot de passe
     */
    void resetPassword(String token, String newPassword);
}
