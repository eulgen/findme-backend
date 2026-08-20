package com.geolink.findme.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de requête pour la mise à jour des informations de profil.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequestDTO {

    @NotBlank(message = "Le nom complet est obligatoire")
    private String fullName;

    private String phoneNumber;
}
