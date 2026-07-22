package com.example.findme.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Intercepteur global des exceptions lancees par les controleurs.
 *
 * <p>Convertit les exceptions Java en reponses HTTP standardisees
 * ({@link ApiErrorResponse}) pour les clients REST.</p>
 *
 * @author findme-team
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gere les erreurs 404 (Resource Not Found).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        log.warn("Resource introuvable : {} - URL : {}", ex.getMessage(), request.getRequestURI());
        
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Gere les erreurs 409 (Conflict).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {
        
        log.warn("Conflit de ressource : {} - URL : {}", ex.getMessage(), request.getRequestURI());
        
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Gere les erreurs 400 (Bad Request - echec de validation des DTO).
     * <p>Extrait tous les champs en erreur et leurs messages pour les inclure
     * dans la reponse ({@code validationErrors}).</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        
        log.warn("Erreur de validation sur {} champs - URL : {}", errors.size(), request.getRequestURI());
        
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Echec de la validation des donnees de la requete")
                .path(request.getRequestURI())
                .validationErrors(errors)
                .build();
                
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Gere les erreurs de limite metier atteinte (ex: 4 adresses max).
     */
    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleLimitExceededException(
            LimitExceededException ex, HttpServletRequest request) {
        
        log.warn("Limite atteinte : {} - URL : {}", ex.getMessage(), request.getRequestURI());
        
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Gere toutes les autres exceptions non prevues (500 Internal Server Error).
     * Empeche l'envoi de stacktraces Java directement au client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGlobalException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Erreur serveur inattendue - URL : {}", request.getRequestURI(), ex);
        
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Une erreur inattendue s'est produite sur le serveur")
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
