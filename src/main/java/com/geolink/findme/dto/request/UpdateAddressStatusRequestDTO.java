package com.geolink.findme.dto.request;

import com.geolink.findme.entity.AddressStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de requête pour la mise à jour du statut d'une adresse par l'administrateur.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAddressStatusRequestDTO {

    @NotNull(message = "Le statut est obligatoire (EN_ATTENTE, VALIDE, NON_VALIDE)")
    private AddressStatus status;
}
