package com.geolink.findme.exception;

/**
 * Exception levée lorsqu'une adresse demandée n'existe pas en base de données.
 */
public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException(String message) {
        super(message);
    }
}
