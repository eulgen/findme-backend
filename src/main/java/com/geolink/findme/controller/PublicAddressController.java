package com.geolink.findme.controller;

import com.geolink.findme.dto.request.AddressRequestDTO;
import com.geolink.findme.dto.response.AddressResponseDTO;
import com.geolink.findme.service.addressService.AddressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST assurant la consultation et création publique des adresses (/api/public/addresses).
 * Permet de créer et consulter une adresse sans compte ni authentification.
 */
@RestController
@RequestMapping("/api/public/addresses")
@RequiredArgsConstructor
@Tag(name = "Adresses Publiques", description = "Endpoints publics de création et consultation des adresses sans compte ni authentification")
public class PublicAddressController {

    private final AddressService addressService;

    @PostMapping
    @Operation(
            summary = "Créer une adresse de manière anonyme",
            description = "Crée une nouvelle adresse sans compte utilisateur et retourne l'adresse avec son addressCode unique"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Adresse publique créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Données d'entrée invalides")
    })
    public ResponseEntity<AddressResponseDTO> createPublicAddress(
            @Valid @RequestBody AddressRequestDTO requestDTO
    ) {
        AddressResponseDTO createdAddress = addressService.createPublicAddress(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAddress);
    }

    @GetMapping("/{addressCode}")
    @Operation(
            summary = "Consulter les informations d'une adresse par son code",
            description = "Permet à tout utilisateur non connecté d'obtenir l'ensemble des détails d'une adresse via son addressCode unique"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Détails de l'adresse récupérés avec succès"),
            @ApiResponse(responseCode = "404", description = "Adresse non trouvée pour le code fourni")
    })
    public ResponseEntity<AddressResponseDTO> getAddressByCode(
            @Parameter(description = "Code unique de l'adresse (ex: ADR-2026-X1Y2)", required = true)
            @PathVariable String addressCode
    ) {
        return ResponseEntity.ok(addressService.getAddressByCode(addressCode));
    }
}
