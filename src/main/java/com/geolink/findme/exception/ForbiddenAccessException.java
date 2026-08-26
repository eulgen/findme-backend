package com.geolink.findme.exception;

/**
 * Exception levée lorsqu'un utilisateur tente d'accéder ou modifier une ressource ne lui appartenant pas.
 */
public class ForbiddenAccessException extends RuntimeException {

    public ForbiddenAccessException(String message) {
        super(message);
    }
}
