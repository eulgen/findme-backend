package com.geolink.findme.controller;

import com.geolink.findme.dto.request.RoleUpdateDTO;
import com.geolink.findme.dto.response.UserProfileDTO;
import com.geolink.findme.service.adminService.AdminService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST d'administration pour la consultation de tous les utilisateurs.
 * Accès réservé exclusivement au rôle ADMIN.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration - Utilisateurs", description = "Endpoints d'administration réservés aux administrateurs (ADMIN)")
public class AdminUserController {

    private final AdminService adminService;

    @GetMapping
    @Operation(summary = "Liste des utilisateurs (Admin)", description = "Retourne la liste paginée de tous les utilisateurs de la plateforme, avec filtre optionnel par nom ou email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des utilisateurs récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé - Réservé aux administrateurs (ADMIN)")
    })
    public ResponseEntity<Page<UserProfileDTO>> getAllUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getAllUsers(search, pageable));
    }

    @PutMapping("/{userId}/role")
    @Operation(summary = "Mettre à jour le rôle d'un utilisateur", description = "Permet à un administrateur de changer le rôle (ex: USER, ADMIN, SUPPORT_AGENT) d'un utilisateur existant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rôle mis à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé - Réservé aux administrateurs (ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Utilisateur ou Rôle non trouvé")
    })
    public ResponseEntity<Void> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody RoleUpdateDTO roleUpdateDTO
    ) {
        adminService.updateUserRole(userId, roleUpdateDTO.getRoleName());
        return ResponseEntity.ok().build();
    }
}
