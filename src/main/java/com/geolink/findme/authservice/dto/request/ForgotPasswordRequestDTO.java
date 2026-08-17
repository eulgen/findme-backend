package com.geolink.findme.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requête pour demander une réinitialisation de mot de passe.
 */
public class ForgotPasswordRequestDTO {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    public ForgotPasswordRequestDTO() {
    }

    public ForgotPasswordRequestDTO(String email) {
        this.email = email;
    }

    public static ForgotPasswordRequestDTOBuilder builder() {
        return new ForgotPasswordRequestDTOBuilder();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public static class ForgotPasswordRequestDTOBuilder {
        private String email;

        public ForgotPasswordRequestDTOBuilder email(String email) {
            this.email = email;
            return this;
        }

        public ForgotPasswordRequestDTO build() {
            return new ForgotPasswordRequestDTO(email);
        }
    }
}
