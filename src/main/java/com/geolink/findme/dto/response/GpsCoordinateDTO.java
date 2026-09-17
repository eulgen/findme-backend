package com.geolink.findme.dto.response;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO représentant les coordonnées GPS (latitude, longitude).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpsCoordinateDTO {

    @NotNull(message = "La latitude est obligatoire")
    @Min(value = -90, message = "La latitude doit être supérieure ou égale à -90")
    @Max(value = 90, message = "La latitude doit être inférieure ou égale à 90")
    private Double latitude;

    @NotNull(message = "La longitude est obligatoire")
    @Min(value = -180, message = "La longitude doit être supérieure ou égale à -180")
    @Max(value = 180, message = "La longitude doit être inférieure ou égale à 180")
    private Double longitude;
}
