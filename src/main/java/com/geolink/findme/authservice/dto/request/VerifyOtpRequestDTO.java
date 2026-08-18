package com.geolink.findme.authservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "DTO de demande de vérification de compte via OTP")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyOtpRequestDTO {

    @NotBlank(message = "L'email ne peut pas être vide")
    @Email(message = "L'adresse email doit être valide")
    @Schema(description = "Adresse email de l'utilisateur", example = "user@example.com")
    private String email;

    @NotBlank(message = "Le code OTP est obligatoire")
    @Size(min = 6, max = 6, message = "Le code OTP doit comporter 6 chiffres")
    @Schema(description = "Code OTP à 6 chiffres reçu par mail", example = "123456")
    private String code;
}
