package com.geolink.findme.exception;

/**
 * Exception levée lorsqu'un message de support recherché est introuvable (HTTP 404).
 */
public class SupportMessageNotFoundException extends RuntimeException {
    public SupportMessageNotFoundException(String message) {
        super(message);
    }
}
