package com.geolink.findme.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requête pour le rafraîchissement des tokens d'accès.
 */
public class RefreshRequestDTO {

    @NotBlank(message = "Le token de rafraîchissement est obligatoire")
    private String refreshToken;

    public RefreshRequestDTO() {
    }

    public RefreshRequestDTO(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public static RefreshRequestDTOBuilder builder() {
        return new RefreshRequestDTOBuilder();
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public static class RefreshRequestDTOBuilder {
        private String refreshToken;

        public RefreshRequestDTOBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public RefreshRequestDTO build() {
            return new RefreshRequestDTO(refreshToken);
        }
    }
}
