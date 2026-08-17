package com.geolink.findme.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO de requête pour l'inscription d'un nouvel utilisateur.
 */
public class SignUpRequestDTO {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule et un chiffre"
    )
    private String password;

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    public SignUpRequestDTO() {
    }

    public SignUpRequestDTO(String email, String password, String firstName, String lastName) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public static SignUpRequestDTOBuilder builder() {
        return new SignUpRequestDTOBuilder();
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public static class SignUpRequestDTOBuilder {
        private String email;
        private String password;
        private String firstName;
        private String lastName;

        public SignUpRequestDTOBuilder email(String email) {
            this.email = email;
            return this;
        }

        public SignUpRequestDTOBuilder password(String password) {
            this.password = password;
            return this;
        }

        public SignUpRequestDTOBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public SignUpRequestDTOBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public SignUpRequestDTO build() {
            return new SignUpRequestDTO(email, password, firstName, lastName);
        }
    }
}
