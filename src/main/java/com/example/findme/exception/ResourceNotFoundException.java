package com.example.findme.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levee lorsqu'une ressource (utilisateur, adresse, etc.)
 * demandee est introuvable.
 *
 * <p>Produira une reponse HTTP 404 (Not Found).</p>
 *
 * @author findme-team
 * @version 1.0.0
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
