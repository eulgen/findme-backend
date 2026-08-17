package com.geolink.findme.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requête pour la connexion par email / mot de passe.
 */
public class SignInRequestDTO {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;

    public SignInRequestDTO() {
    }

    public SignInRequestDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public static SignInRequestDTOBuilder builder() {
        return new SignInRequestDTOBuilder();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static class SignInRequestDTOBuilder {
        private String email;
        private String password;

        public SignInRequestDTOBuilder email(String email) {
            this.email = email;
            return this;
        }

        public SignInRequestDTOBuilder password(String password) {
            this.password = password;
            return this;
        }

        public SignInRequestDTO build() {
            return new SignInRequestDTO(email, password);
        }
    }
}
