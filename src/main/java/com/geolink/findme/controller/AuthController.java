package com.geolink.findme.controller;

import com.geolink.findme.dto.request.AppleLoginRequestDTO;
import com.geolink.findme.dto.request.ForgotPasswordRequestDTO;
import com.geolink.findme.dto.request.GoogleLoginRequestDTO;
import com.geolink.findme.dto.request.RefreshRequestDTO;
import com.geolink.findme.dto.request.ResendOtpRequestDTO;
import com.geolink.findme.dto.request.ResetPasswordRequestDTO;
import com.geolink.findme.dto.request.SignInRequestDTO;
import com.geolink.findme.dto.request.SignUpRequestDTO;
import com.geolink.findme.dto.request.VerifyOtpRequestDTO;
import com.geolink.findme.dto.response.AuthResponseDTO;
import com.geolink.findme.dto.response.UserProfileDTO;
import com.geolink.findme.service.auth.AuthService;
import com.geolink.findme.service.auth.SocialAuthService;
import com.geolink.findme.service.passwordService.PasswordResetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.Principal;

/**
 * Contrôleur REST pour l'authentification (classique, OTP et sociale Google/Apple).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Endpoints d'authentification (Standard, OTP, Google & Apple Sign-In)")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final SocialAuthService socialAuthService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscription utilisateur", description = "Crée un compte utilisateur non vérifié et envoie un code OTP par mail")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte créé avec succès, mail d'OTP envoyé"),
            @ApiResponse(responseCode = "400", description = "Données d'entrée invalides"),
            @ApiResponse(responseCode = "409", description = "Adresse email déjà utilisée")
    })
    public UserProfileDTO signUp(@Valid @RequestBody SignUpRequestDTO dto) {
        return authService.signUp(dto);
    }

    @PostMapping("/verify-account")
    @Operation(summary = "Vérification du compte par OTP", description = "Valide le code OTP à 6 chiffres reçu par email et active le compte")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte vérifié avec succès"),
            @ApiResponse(responseCode = "400", description = "Code OTP incorrect ou expiré")
    })
    public ResponseEntity<Void> verifyAccount(@Valid @RequestBody VerifyOtpRequestDTO dto) {
        authService.verifyAccount(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Renvoi de l'OTP de vérification", description = "Génère et renvoie un nouveau code OTP de vérification de compte")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nouveau code OTP envoyé"),
            @ApiResponse(responseCode = "400", description = "Compte déjà vérifié ou inexistant")
    })
    public ResponseEntity<Void> resendOtp(@Valid @RequestBody ResendOtpRequestDTO dto) {
        authService.resendVerificationOtp(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/signin")
    @Operation(summary = "Connexion utilisateur", description = "Authentifie l'utilisateur et retourne un pair de tokens (Access + Refresh)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie"),
            @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect, ou compte non vérifié")
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
    @Operation(summary = "Déconnexion utilisateur", description = "Révoque le refresh token transmis dans le corps de la requête ou toutes les sessions de l'utilisateur connecté")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Déconnexion réussie"),
            @ApiResponse(responseCode = "400", description = "Requête invalide")
    })
    public void logout(@RequestBody(required = false) RefreshRequestDTO dto, Principal principal) {
        if (dto != null && dto.getRefreshToken() != null && !dto.getRefreshToken().isBlank()) {
            authService.logoutWithToken(dto.getRefreshToken());
        } else if (principal != null) {
            authService.logout(principal.getName());
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Demande de réinitialisation de mot de passe", description = "Génère un code OTP de réinitialisation envoyé par mail (réponse 200 systématique pour prévenir l'énumération de comptes)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande prise en compte")
    })
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        passwordResetService.requestReset(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialisation du mot de passe", description = "Définit un nouveau mot de passe via le code OTP de réinitialisation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe réinitialisé avec succès"),
            @ApiResponse(responseCode = "400", description = "Code OTP invalide ou expiré")
    })
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        passwordResetService.resetPassword(dto.getEmail(), dto.getCode(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // ENDPOINTS AUTHENTIFICATION SOCIALE GOOGLE & APPLE
    // =========================================================================

    @GetMapping("/google/login")
    @Operation(summary = "Connexion Google (Redirection Web)", description = "Redirige le navigateur vers la page d'autorisation Google OAuth2")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirection vers Google OAuth2")
    })
    public void googleLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/apple/login")
    @Operation(summary = "Connexion Apple (Redirection Web)", description = "Redirige le navigateur vers la page d'autorisation Sign in with Apple")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirection vers Apple OIDC")
    })
    public void appleLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/apple");
    }

    @PostMapping("/google")
    @Operation(summary = "Connexion Google par ID Token (Mobile / SPA)", description = "Authentifie l'utilisateur via son idToken Google et émet un pair de tokens (Access + Refresh)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentification Google réussie"),
            @ApiResponse(responseCode = "401", description = "Jeton Google invalide")
    })
    public ResponseEntity<AuthResponseDTO> authenticateGoogle(@Valid @RequestBody GoogleLoginRequestDTO dto) {
        return ResponseEntity.ok(socialAuthService.authenticateWithGoogle(dto));
    }

    @PostMapping("/apple")
    @Operation(summary = "Connexion Apple par ID Token (Mobile / SPA)", description = "Authentifie l'utilisateur via son idToken Apple et émet un pair de tokens (Access + Refresh)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentification Apple réussie"),
            @ApiResponse(responseCode = "401", description = "Jeton Apple invalide")
    })
    public ResponseEntity<AuthResponseDTO> authenticateApple(@Valid @RequestBody AppleLoginRequestDTO dto) {
        return ResponseEntity.ok(socialAuthService.authenticateWithApple(dto));
    }
}
