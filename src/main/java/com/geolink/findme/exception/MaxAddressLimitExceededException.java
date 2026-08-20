package com.geolink.findme.exception;

/**
 * Exception levée lorsqu'un utilisateur tente de créer plus de 4 adresses (limite projet).
 */
public class MaxAddressLimitExceededException extends RuntimeException {

    public MaxAddressLimitExceededException(String message) {
        super(message);
    }
}
