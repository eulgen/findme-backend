package com.geolink.findme.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire d'exceptions global REST retournant des réponses standardisées au format RFC 7807 (ProblemDetail).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ProblemDetail handleEmailAlreadyUsed(EmailAlreadyUsedException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://api.findme.geolink.com/errors/email-already-used"));
        pd.setTitle("Adresse email déjà utilisée");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setType(URI.create("https://api.findme.geolink.com/errors/invalid-credentials"));
        pd.setTitle("Identifiants invalides");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(InvalidOrExpiredTokenException.class)
    public ProblemDetail handleInvalidOrExpiredToken(InvalidOrExpiredTokenException ex, HttpServletRequest request) {
        HttpStatus status = ex.isExpired() ? HttpStatus.GONE : HttpStatus.BAD_REQUEST;
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setType(URI.create("https://api.findme.geolink.com/errors/invalid-or-expired-token"));
        pd.setTitle(ex.isExpired() ? "Token expiré" : "Token invalide");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("https://api.findme.geolink.com/errors/user-not-found"));
        pd.setTitle("Utilisateur non trouvé");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(AddressNotFoundException.class)
    public ProblemDetail handleAddressNotFound(AddressNotFoundException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("https://api.findme.geolink.com/errors/address-not-found"));
        pd.setTitle("Adresse non trouvée");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ProblemDetail handleRoleNotFound(RoleNotFoundException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("https://api.findme.geolink.com/errors/role-not-found"));
        pd.setTitle("Rôle introuvable");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(SupportMessageNotFoundException.class)
    public ProblemDetail handleSupportMessageNotFound(SupportMessageNotFoundException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("https://api.findme.geolink.com/errors/support-message-not-found"));
        pd.setTitle("Message de support non trouvé");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(MaxAddressLimitExceededException.class)
    public ProblemDetail handleMaxAddressLimitExceeded(MaxAddressLimitExceededException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://api.findme.geolink.com/errors/max-address-limit-exceeded"));
        pd.setTitle("Limite d'adresses atteinte");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(ForbiddenAccessException.class)
    public ProblemDetail handleForbiddenAccess(ForbiddenAccessException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setType(URI.create("https://api.findme.geolink.com/errors/forbidden-access"));
        pd.setTitle("Accès interdit à l'adresse");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(InvalidFileException.class)
    public ProblemDetail handleInvalidFile(InvalidFileException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("https://api.findme.geolink.com/errors/invalid-file"));
        pd.setTitle("Fichier invalide");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Erreur de validation des données d'entrée");
        pd.setType(URI.create("https://api.findme.geolink.com/errors/validation-error"));
        pd.setTitle("Échec de validation");
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("invalidParams", errors);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Le corps de la requête (JSON) est obligatoire ou mal formé");
        pd.setType(URI.create("https://api.findme.geolink.com/errors/malformed-json"));
        pd.setTitle("Corps de requête invalide");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.toString());
        pd.setType(URI.create("https://api.findme.geolink.com/errors/internal-error"));
        pd.setTitle("Erreur serveur");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }
}
