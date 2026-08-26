package com.geolink.findme.exception;

/**
 * Exception levée lorsqu'un fichier téléversé est invalide (type non supporté, taille excessive, vide).
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
