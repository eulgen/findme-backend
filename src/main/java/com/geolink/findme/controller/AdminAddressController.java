package com.geolink.findme.controller;

import com.geolink.findme.dto.response.AddressResponseDTO;
import com.geolink.findme.service.adminService.AdminService;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST d'administration pour la vue globale des adresses.
 * Accès réservé exclusivement au rôle ADMIN.
 */
@RestController
@RequestMapping("/api/admin/addresses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration - Adresses", description = "Endpoints de vue globale des adresses réservés aux administrateurs (ADMIN)")
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
}
