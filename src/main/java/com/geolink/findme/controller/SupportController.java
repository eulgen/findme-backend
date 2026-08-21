package com.geolink.findme.controller;

import com.geolink.findme.dto.request.CreateSupportRequestDTO;
import com.geolink.findme.dto.response.SupportResponseDTO;
import com.geolink.findme.service.supportService.SupportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Contrôleur REST public pour l'envoi de messages de support client.
 */
@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Tag(name = "Support Client", description = "Endpoints de gestion du formulaire public de support client")
public class SupportController {

    private final SupportService supportService;

    @PostMapping
    @Operation(summary = "Envoi d'un message de support", description = "Soumission d'une demande de support via le formulaire public (nom, email, message)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message de support créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données du formulaire invalides")
    })
    public ResponseEntity<SupportResponseDTO> createSupportMessage(
            @Valid @RequestBody CreateSupportRequestDTO requestDTO,
            Principal principal
    ) {
        String authenticatedEmail = (principal != null) ? principal.getName() : null;
        SupportResponseDTO createdMessage = supportService.createSupportMessage(requestDTO, authenticatedEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMessage);
    }
}
