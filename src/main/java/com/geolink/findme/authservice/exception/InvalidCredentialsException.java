package com.geolink.findme.authservice.exception;

/**
 * Exception levée lors d'un échec d'authentification par email/mot de passe (HTTP 401).
 * Message générique pour éviter l'énumération de comptes.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
