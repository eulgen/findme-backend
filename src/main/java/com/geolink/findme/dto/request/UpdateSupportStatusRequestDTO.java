package com.geolink.findme.dto.request;

import com.geolink.findme.entity.SupportStatus;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de requête pour la mise à jour du statut d'un message de support (ex: PENDING -> PROCESSED).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSupportStatusRequestDTO {

    @NotNull(message = "Le statut est obligatoire")
    private SupportStatus status;
}
