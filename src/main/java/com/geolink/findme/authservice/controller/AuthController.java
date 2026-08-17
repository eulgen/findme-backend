package com.geolink.findme.authservice.controller;

import com.geolink.findme.authservice.dto.request.ForgotPasswordRequestDTO;
import com.geolink.findme.authservice.dto.request.RefreshRequestDTO;
import com.geolink.findme.authservice.dto.request.ResetPasswordRequestDTO;
import com.geolink.findme.authservice.dto.request.SignInRequestDTO;
import com.geolink.findme.authservice.dto.request.SignUpRequestDTO;
import com.geolink.findme.authservice.dto.response.AuthResponseDTO;
import com.geolink.findme.authservice.dto.response.UserProfileDTO;
import com.geolink.findme.authservice.service.auth.AuthService;
import com.geolink.findme.authservice.service.passwordService.PasswordResetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Contrôleur REST pour l'authentification, la gestion de session et la réinitialisation de mot de passe.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Endpoints d'authentification, de gestion de session et de mot de passe")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscription utilisateur", description = "Crée un nouveau compte utilisateur avec le rôle USER par défaut")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données d'entrée invalides"),
            @ApiResponse(responseCode = "409", description = "Adresse email déjà utilisée")
    })
    public UserProfileDTO signUp(@Valid @RequestBody SignUpRequestDTO dto) {
        return authService.signUp(dto);
    }

    @PostMapping("/signin")
    @Operation(summary = "Connexion utilisateur", description = "Authentifie l'utilisateur et retourne un pair de tokens (Access + Refresh)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie"),
            @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect")
    })
    public ResponseEntity<AuthResponseDTO> signIn(@Valid @RequestBody SignInRequestDTO dto) {
        return ResponseEntity.ok(authService.signIn(dto));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchissement des tokens", description = "Génère un nouvel access token et effectue la rotation du refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens renouvelés avec succès"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalide ou expiré")
    })
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO dto) {
        return ResponseEntity.ok(authService.refresh(dto));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Déconnexion utilisateur", description = "Révoque tous les refresh tokens actifs de l'utilisateur")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Déconnexion réussie"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public void logout(Principal principal) {
        if (principal != null) {
            authService.logout(principal.getName());
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Demande de réinitialisation de mot de passe", description = "Génère un token de réinitialisation (réponse 200 systématique pour prévenir l'énumération de comptes)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande prise en compte")
    })
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        passwordResetService.requestReset(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialisation du mot de passe", description = "Définit un nouveau mot de passe via le token de réinitialisation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe réinitialisé avec succès"),
            @ApiResponse(responseCode = "400", description = "Token invalide ou déjà utilisé"),
            @ApiResponse(responseCode = "410", description = "Token expiré")
    })
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        passwordResetService.resetPassword(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }
}
