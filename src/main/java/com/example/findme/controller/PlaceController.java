package com.example.findme.controller;

import com.example.findme.dto.request.PlaceRequest;
import com.example.findme.dto.response.PlaceResponse;
import com.example.findme.service.PlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Controleur REST gerant les Lieux (Places) de l'utilisateur.
 *
 * <p>Ces endpoints necessitent un utilisateur authentifie.
 * L'identite de l'utilisateur courant est recuperee via l'objet {@link Principal}.</p>
 *
 * @author findme-team
 */
@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @PostMapping
    public ResponseEntity<PlaceResponse> createPlace(@Valid @RequestBody PlaceRequest request, Principal principal) {
        PlaceResponse response = placeService.createPlace(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PlaceResponse>> getUserPlaces(Principal principal) {
        List<PlaceResponse> responses = placeService.getUserPlaces(principal.getName());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaceResponse> getPlaceById(@PathVariable Long id, Principal principal) {
        PlaceResponse response = placeService.getPlaceById(id, principal.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlaceResponse> updatePlace(@PathVariable Long id, @Valid @RequestBody PlaceRequest request, Principal principal) {
        PlaceResponse response = placeService.updatePlace(id, request, principal.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlace(@PathVariable Long id, Principal principal) {
        placeService.deletePlace(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
