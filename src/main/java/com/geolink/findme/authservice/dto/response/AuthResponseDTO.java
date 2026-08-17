package com.geolink.findme.authservice.dto.response;

/**
 * DTO de réponse lors d'une authentification ou d'un rafraîchissement réussi.
 */
public class AuthResponseDTO {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(String accessToken, String refreshToken, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public static AuthResponseDTOBuilder builder() {
        return new AuthResponseDTOBuilder();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public static class AuthResponseDTOBuilder {
        private String accessToken;
        private String refreshToken;
        private long expiresIn;

        public AuthResponseDTOBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public AuthResponseDTOBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public AuthResponseDTOBuilder expiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public AuthResponseDTO build() {
            return new AuthResponseDTO(accessToken, refreshToken, expiresIn);
        }
    }
}
