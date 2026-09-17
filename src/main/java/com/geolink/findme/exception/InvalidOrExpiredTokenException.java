package com.geolink.findme.exception;

/**
 * Exception levée lorsqu'un refresh token ou reset token est invalide ou expiré.
 */
public class InvalidOrExpiredTokenException extends RuntimeException {
    private final boolean expired;

    public InvalidOrExpiredTokenException(String message) {
        this(message, false);
    }

    public InvalidOrExpiredTokenException(String message, boolean expired) {
        super(message);
        this.expired = expired;
    }

    public boolean isExpired() {
        return expired;
    }
}
