package com.geolink.findme.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requête pour la mise à jour des informations de profil.
 */
public class UpdateProfileRequestDTO {

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    public UpdateProfileRequestDTO() {
    }

    public UpdateProfileRequestDTO(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public static UpdateProfileRequestDTOBuilder builder() {
        return new UpdateProfileRequestDTOBuilder();
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

    public static class UpdateProfileRequestDTOBuilder {
        private String firstName;
        private String lastName;

        public UpdateProfileRequestDTOBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UpdateProfileRequestDTOBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UpdateProfileRequestDTO build() {
            return new UpdateProfileRequestDTO(firstName, lastName);
        }
    }
}
