package com.geolink.findme.controller;

import com.geolink.findme.dto.request.UpdateAddressStatusRequestDTO;
import com.geolink.findme.dto.response.AddressResponseDTO;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contrôleur REST d'administration pour la gestion des adresses.
 * Accès réservé exclusivement au rôle ADMIN.
 */
@RestController
@RequestMapping("/api/admin/addresses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration - Adresses", description = "Endpoints de gestion des adresses réservés aux administrateurs (ADMIN)")
public class AdminAddressController {

    private final AdminService adminService;

    @GetMapping
    @Operation(summary = "Liste globale des adresses (Admin)", description = "Retourne la liste paginée de toutes les adresses de la plateforme, avec filtres par pays, ville et quartier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des adresses récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé - Réservé aux administrateurs (ADMIN)")
    })
    public ResponseEntity<Page<AddressResponseDTO>> getAllAddresses(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getAllAddresses(country, city, district, pageable));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Valider / modifier le statut d'une adresse (Admin)", description = "Permet à l'administrateur d'attribuer un statut (EN_ATTENTE, VALIDE, NON_VALIDE) à une adresse.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut de l'adresse mis à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé - Réservé aux administrateurs (ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Adresse non trouvée")
    })
    public ResponseEntity<AddressResponseDTO> updateAddressStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAddressStatusRequestDTO requestDTO
    ) {
        return ResponseEntity.ok(adminService.updateAddressStatus(id, requestDTO.getStatus()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une adresse (Admin)", description = "Permet à un administrateur de supprimer définitivement une adresse.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Adresse supprimée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé - Réservé aux administrateurs (ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Adresse non trouvée")
    })
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        adminService.deleteAddress(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/users")
    @Operation(summary = "Trouver les informations utilisateur d'une adresse (Admin)", description = "Retourne la liste des informations utilisateurs (propriétaires) associés à une adresse spécifique.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Informations de l'utilisateur récupérées avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé - Réservé aux administrateurs (ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Adresse non trouvée")
    })
    public ResponseEntity<List<UserProfileDTO>> getUsersByAddressId(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUsersByAddressId(id));
    }
}
