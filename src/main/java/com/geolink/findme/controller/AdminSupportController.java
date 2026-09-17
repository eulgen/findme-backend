package com.geolink.findme.controller;

import com.geolink.findme.dto.request.UpdateSupportStatusRequestDTO;
import com.geolink.findme.dto.response.SupportResponseDTO;
import com.geolink.findme.entity.SupportStatus;
import com.geolink.findme.service.supportService.SupportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST d'administration pour la consultation et le traitement des messages de support.
 * Accès réservé aux rôles ADMIN et SUPPORT_AGENT.
 */
@RestController
@RequestMapping("/api/admin/support")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
@Tag(name = "Administration - Support", description = "Endpoints de gestion et traitement des messages de support (ADMIN & SUPPORT_AGENT)")
public class AdminSupportController {

    private final SupportService supportService;

    @GetMapping
    @Operation(summary = "Liste des messages de support", description = "Retourne la liste paginée des messages de support reçus, filtrable par statut (PENDING / PROCESSED)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé - Privilèges insuffisants")
    })
    public ResponseEntity<Page<SupportResponseDTO>> getSupportMessages(
            @RequestParam(required = false) SupportStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(supportService.getSupportMessages(status, pageable));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Mise à jour du statut d'un message", description = "Permet de modifier le statut d'un ticket de support (ex: PENDING -> PROCESSED)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut mis à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Message de support non trouvé")
    })
    public ResponseEntity<SupportResponseDTO> updateSupportStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupportStatusRequestDTO requestDTO
    ) {
        return ResponseEntity.ok(supportService.updateSupportStatus(id, requestDTO));
    }
}
