package com.geolink.findme.controller;

import com.geolink.findme.dto.request.UpdateProfileRequestDTO;
import com.geolink.findme.dto.response.UserProfileDTO;
import com.geolink.findme.service.userService.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

/**
 * Contrôleur REST pour la consultation et la modification du profil utilisateur.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Endpoints de consultation et modification du profil utilisateur")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Obtenir le profil courant", description = "Retourne les informations du profil de l'utilisateur connecté")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil récupéré avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<UserProfileDTO> getCurrentUser(Principal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getName()));
    }

    @PutMapping("/me")
    @Operation(summary = "Mettre à jour le profil", description = "Met à jour les informations (prénom, nom) de l'utilisateur connecté")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil mis à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<UserProfileDTO> updateCurrentUser(
            Principal principal,
            @Valid @RequestBody UpdateProfileRequestDTO dto
    ) {
        return ResponseEntity.ok(userService.updateProfile(principal.getName(), dto));
    }

    @PostMapping(value = {"/me/profile-image", "/me/avatar"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de l'image de profil", description = "Téléverse ou remplace l'image de profil de l'utilisateur connecté (JPEG, PNG, WEBP, max 5 Mo)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image de profil mise à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Fichier invalide (format non supporté ou taille > 5 Mo)"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<UserProfileDTO> uploadProfileImage(
            Principal principal,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(userService.uploadProfileImage(principal.getName(), file));
    }
}
