package com.geolink.findme.exception;

/**
 * Exception levée lorsqu'une adresse email est déjà utilisée lors de l'inscription (HTTP 409).
 */
public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException(String message) {
        super(message);
    }
}
