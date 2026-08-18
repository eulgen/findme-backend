package com.geolink.findme.authservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "DTO de demande de renvoi du code OTP de vérification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendOtpRequestDTO {

    @NotBlank(message = "L'email ne peut pas être vide")
    @Email(message = "L'adresse email doit être valide")
    @Schema(description = "Adresse email de l'utilisateur", example = "user@example.com")
    private String email;
}
