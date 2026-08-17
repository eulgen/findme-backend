package com.geolink.findme.authservice.dto.response;

import java.time.Instant;

/**
 * DTO de réponse représentant le profil public d'un utilisateur.
 * Le champ {@code role} est le rôle principal calculé (ex. USER, ADMIN).
 */
public class UserProfileDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String status;
    private Instant createdAt;
    private Instant lastLoginAt;

    public UserProfileDTO() {
    }

    public UserProfileDTO(Long id, String email, String firstName, String lastName,
                          String role, String status, Instant createdAt, Instant lastLoginAt) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public static UserProfileDTOBuilder builder() {
        return new UserProfileDTOBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public static class UserProfileDTOBuilder {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private String status;
        private Instant createdAt;
        private Instant lastLoginAt;

        public UserProfileDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserProfileDTOBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserProfileDTOBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserProfileDTOBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserProfileDTOBuilder role(String role) {
            this.role = role;
            return this;
        }

        public UserProfileDTOBuilder status(String status) {
            this.status = status;
            return this;
        }

        public UserProfileDTOBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserProfileDTOBuilder lastLoginAt(Instant lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
            return this;
        }

        public UserProfileDTO build() {
            return new UserProfileDTO(id, email, firstName, lastName, role, status, createdAt, lastLoginAt);
        }
    }
}
