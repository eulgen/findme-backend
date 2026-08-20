package com.geolink.findme.dto.request;

import com.geolink.findme.dto.response.GpsCoordinateDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de requête pour la création d'une nouvelle adresse.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequestDTO {

    @NotBlank(message = "Le pays est obligatoire")
    @Size(max = 100, message = "Le pays ne peut pas dépasser 100 caractères")
    private String country;

    @NotBlank(message = "La ville est obligatoire")
    @Size(max = 100, message = "La ville ne peut pas dépasser 100 caractères")
    private String city;

    @NotBlank(message = "La rue est obligatoire")
    @Size(max = 150, message = "La rue ne peut pas dépasser 150 caractères")
    private String street;

    @Size(max = 4, message = "Le code postal ne peut pas dépasser 4 caractères")
    private String postalCode;

    @NotBlank(message = "L'URL de la photo est obligatoire")
    @Size(max = 200, message = "L'URL de la photo ne peut pas dépasser 200 caractères")
    private String photoUrl;

    @Size(max = 50, message = "Le numéro de maison ne peut pas dépasser 50 caractères")
    private String houseNumber;

    @NotNull(message = "Les coordonnées GPS sont obligatoires")
    @Valid
    private GpsCoordinateDTO gps;
}
