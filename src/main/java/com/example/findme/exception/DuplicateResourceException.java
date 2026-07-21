package com.example.findme.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levee lors d'une tentative de creation d'une ressource
 * qui viole une contrainte d'unicite (ex: email ou username deja utilise).
 *
 * <p>Produira une reponse HTTP 409 (Conflict).</p>
 *
 * @author findme-team
 * @version 1.0.0
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
