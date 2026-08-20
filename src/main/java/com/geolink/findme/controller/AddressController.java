package com.geolink.findme.controller;

import com.geolink.findme.dto.request.AddressRequestDTO;
import com.geolink.findme.dto.response.AddressExportDTO;
import com.geolink.findme.dto.response.AddressResponseDTO;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.InvalidCredentialsException;
import com.geolink.findme.exception.UserNotFoundException;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.security.UserPrincipal;
import com.geolink.findme.service.addressService.AddressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

/**
 * Contrôleur REST assurant la gestion complète des adresses (/api/addresses).
 */
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(name = "Adresses", description = "Endpoints de gestion des adresses de l'utilisateur (CRUD, photo, export PDF)")
public class AddressController {

    private final AddressService addressService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Lister les adresses de l'utilisateur", description = "Retourne la liste paginée, triée et filtrable des adresses de l'utilisateur connecté")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des adresses récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<Page<AddressResponseDTO>> getUserAddresses(
            @Parameter(description = "Filtre par pays") @RequestParam(required = false) String country,
            @Parameter(description = "Filtre par ville") @RequestParam(required = false) String city,
            @Parameter(description = "Filtre par rue") @RequestParam(required = false) String street,
            @Parameter(description = "Recherche textuelle globale (code, ville, rue, numéro)") @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Principal principal,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        User user = resolveUser(principal, userPrincipal);
        Page<AddressResponseDTO> addresses = addressService.getUserAddresses(user, country, city, street, search, pageable);
        return ResponseEntity.ok(addresses);
    }

    @PostMapping
    @Operation(summary = "Créer une adresse", description = "Crée une nouvelle adresse et la rattache à l'utilisateur (blocage strict à 4 adresses max)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Adresse créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Données d'entrée invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "409", description = "Limite maximale de 4 adresses atteinte pour cet utilisateur")
    })
    public ResponseEntity<AddressResponseDTO> createAddress(
            @Valid @RequestBody AddressRequestDTO requestDTO,
            Principal principal,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        User user = resolveUser(principal, userPrincipal);
        AddressResponseDTO createdAddress = addressService.createAddress(user, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAddress);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une adresse", description = "Retourne les détails d'une adresse spécifique si elle appartient à l'utilisateur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Détail de l'adresse récupéré"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé - l'adresse appartient à un autre utilisateur"),
            @ApiResponse(responseCode = "404", description = "Adresse non trouvée")
    })
    public ResponseEntity<AddressResponseDTO> getAddressById(
            @PathVariable Long id,
            Principal principal,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        User user = resolveUser(principal, userPrincipal);
        return ResponseEntity.ok(addressService.getAddressById(user, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une adresse", description = "Met à jour les informations d'une adresse appartenant à l'utilisateur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Adresse mise à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Données d'entrée invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé - l'adresse appartient à un autre utilisateur"),
            @ApiResponse(responseCode = "404", description = "Adresse non trouvée")
    })
    public ResponseEntity<AddressResponseDTO> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequestDTO requestDTO,
            Principal principal,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        User user = resolveUser(principal, userPrincipal);
        return ResponseEntity.ok(addressService.updateAddress(user, id, requestDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une adresse", description = "Supprime le lien d'adresse pour l'utilisateur (et l'adresse si elle devient orpheline)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Adresse supprimée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé - l'adresse appartient à un autre utilisateur"),
            @ApiResponse(responseCode = "404", description = "Adresse non trouvée")
    })
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id,
            Principal principal,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        User user = resolveUser(principal, userPrincipal);
        addressService.deleteAddress(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de photo d'adresse", description = "Téléverse ou remplace l'image associée à une adresse (JPEG, PNG, WEBP, max 5 Mo)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo uploadée avec succès"),
            @ApiResponse(responseCode = "400", description = "Fichier invalide (type non supporté ou taille > 5 Mo)"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé - l'adresse appartient à un autre utilisateur"),
            @ApiResponse(responseCode = "404", description = "Adresse non trouvée")
    })
    public ResponseEntity<AddressResponseDTO> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile file,
            Principal principal,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        User user = resolveUser(principal, userPrincipal);
        return ResponseEntity.ok(addressService.uploadPhoto(user, id, file));
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "Exportation des données pour le PDF frontend", description = "Retourne l'adresse formatée, la position GPS, le code d'adresse et les infos utilisateur pour la génération du PDF côté frontend")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Données d'export générées avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé - l'adresse appartient à un autre utilisateur"),
            @ApiResponse(responseCode = "404", description = "Adresse non trouvée")
    })
    public ResponseEntity<AddressExportDTO> exportAddressPdfData(
            @PathVariable Long id,
            Principal principal,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        User user = resolveUser(principal, userPrincipal);
        return ResponseEntity.ok(addressService.exportAddressPdfData(user, id));
    }

    private User resolveUser(Principal principal, UserPrincipal userPrincipal) {
        if (userPrincipal != null && userPrincipal.getUser() != null) {
            return userRepository.findById(userPrincipal.getUser().getId())
                    .orElse(userPrincipal.getUser());
        }
        if (principal != null && principal.getName() != null) {
            return userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé avec l'email : " + principal.getName()));
        }
        throw new InvalidCredentialsException("Authentification requise pour cette opération");
    }
}
