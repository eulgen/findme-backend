package com.geolink.findme.authservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de requête pour définir un nouveau mot de passe via un code OTP de réinitialisation.
 */
@Schema(description = "DTO de réinitialisation du mot de passe par OTP")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequestDTO {

    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(message = "L'adresse email doit être valide")
    @Schema(description = "Adresse email de l'utilisateur", example = "user@example.com")
    private String email;

    @NotBlank(message = "Le code OTP est obligatoire")
    @Schema(description = "Code OTP à 6 chiffres reçu par mail", example = "654321")
    private String code;

    private String token;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule et un chiffre"
    )
    @Schema(description = "Nouveau mot de passe", example = "NewSecureP@ss123")
    private String newPassword;

    public String getCode() {
        return code != null ? code : token;
    }

    public String getToken() {
        return token != null ? token : code;
    }
}
