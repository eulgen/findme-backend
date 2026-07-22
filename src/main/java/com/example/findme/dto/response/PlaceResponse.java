package com.example.findme.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) representant la vue publique d'un lieu (Place).
 *
 * <p>Utilise comme un {@code Record} Java 14+ pour l'immuabilite.</p>
 *
 * @param id l'identifiant technique
 * @param name le nom du lieu
 * @param address l'adresse complete
 * @param latitude coordonnee GPS (optionnelle)
 * @param longitude coordonnee GPS (optionnelle)
 * @param type categorie de lieu
 * @param createdAt date de creation
 * @param updatedAt date de mise a jour
 */
@Builder
public record PlaceResponse(
        Long id,
        String name,
        String address,
        Double latitude,
        Double longitude,
        String type,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
