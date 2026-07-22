package com.example.findme.controller;

import com.example.findme.dto.request.SigninRequest;
import com.example.findme.dto.request.SignupRequest;
import com.example.findme.dto.response.AuthResponse;
import com.example.findme.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controleur REST gerant l'authentification (Inscription et Connexion).
 *
 * <p>Expose les endpoints correspondant a la collection Postman.</p>
 *
 * @author findme-team
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Endpoint d'inscription d'un nouvel utilisateur.
     *
     * @param request les donnees d'inscription (validees via @Valid)
     * @return AuthResponse (le token JWT et les informations de l'utilisateur) avec le statut 201 Created
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint de connexion d'un utilisateur existant.
     *
     * @param request les donnees de connexion (validees via @Valid)
     * @return AuthResponse (le token JWT et les informations de l'utilisateur) avec le statut 200 OK
     */
    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signin(@Valid @RequestBody SigninRequest request) {
        AuthResponse response = authService.signin(request);
        return ResponseEntity.ok(response);
    }
}
