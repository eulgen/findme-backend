package com.example.findme.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO standardise pour toutes les reponses d'erreur de l'API REST.
 *
 * <p>Garantit un format uniforme pour les clients :</p>
 * <ul>
 *   <li>timestamp : moment de l'erreur</li>
 *   <li>status : code HTTP (ex: 404)</li>
 *   <li>error : nom de l'erreur (ex: Not Found)</li>
 *   <li>message : message detaille pour le developpeur/utilisateur</li>
 *   <li>path : route de l'API appelee</li>
 *   <li>validationErrors : details optionnels si c'est une erreur de validation 400</li>
 * </ul>
 *
 * @author findme-team
 * @version 1.0.0
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    private int status;
    private String error;
    private String message;
    private String path;
    
    // N'est inclus dans le JSON que si non null
    private Map<String, String> validationErrors;
}
