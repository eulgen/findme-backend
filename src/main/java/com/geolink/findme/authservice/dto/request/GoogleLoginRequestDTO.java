package com.geolink.findme.authservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de requête pour l'authentification directe via ID Token Google (Mobile / SPA).
 */
@Schema(description = "DTO d'authentification directe via Google ID Token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleLoginRequestDTO {

    @NotBlank(message = "Le jeton d'identité (idToken) Google est obligatoire")
    @Schema(description = "Jeton d'identité (id_token) émis par le SDK Google Sign-In", example = "eyJhbGciOiJSUzI1NiIs...")
    private String idToken;
}
