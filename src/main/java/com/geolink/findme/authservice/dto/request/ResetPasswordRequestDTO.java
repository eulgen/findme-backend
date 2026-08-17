package com.geolink.findme.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO de requête pour définir un nouveau mot de passe via un token de réinitialisation.
 */
public class ResetPasswordRequestDTO {

    @NotBlank(message = "Le token de réinitialisation est obligatoire")
    private String token;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule et un chiffre"
    )
    private String newPassword;

    public ResetPasswordRequestDTO() {
    }

    public ResetPasswordRequestDTO(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    public static ResetPasswordRequestDTOBuilder builder() {
        return new ResetPasswordRequestDTOBuilder();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public static class ResetPasswordRequestDTOBuilder {
        private String token;
        private String newPassword;

        public ResetPasswordRequestDTOBuilder token(String token) {
            this.token = token;
            return this;
        }

        public ResetPasswordRequestDTOBuilder newPassword(String newPassword) {
            this.newPassword = newPassword;
            return this;
        }

        public ResetPasswordRequestDTO build() {
            return new ResetPasswordRequestDTO(token, newPassword);
        }
    }
}
