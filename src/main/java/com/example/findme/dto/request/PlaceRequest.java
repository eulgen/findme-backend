package com.example.findme.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) utilise pour encapsuler les donnees
 * de creation ou de mise a jour d'un lieu (Place).
 *
 * <p>Assure la validation des champs obligatoires via {@code @Valid}.</p>
 *
 * @author findme-team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceRequest {

    @NotBlank(message = "Le nom du lieu est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas depasser 100 caracteres")
    private String name;

    @NotBlank(message = "L'adresse est obligatoire")
    @Size(max = 255, message = "L'adresse ne doit pas depasser 255 caracteres")
    private String address;

    private Double latitude;

    private Double longitude;

    @Size(max = 50, message = "Le type ne doit pas depasser 50 caracteres")
    private String type;
}
