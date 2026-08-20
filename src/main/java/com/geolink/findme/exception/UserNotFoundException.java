package com.geolink.findme.exception;

/**
 * Exception levée lorsqu'un utilisateur recherché est introuvable (HTTP 404).
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
