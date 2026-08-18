package com.geolink.findme.authservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de requête pour l'authentification directe via ID Token Apple (Mobile / SPA).
 */
@Schema(description = "DTO d'authentification directe via Apple ID Token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppleLoginRequestDTO {

    @NotBlank(message = "Le jeton d'identité (idToken) Apple est obligatoire")
    @Schema(description = "Jeton d'identité (id_token) émis par Sign in with Apple", example = "eyJhbGciOiJSUzI1NiIs...")
    private String idToken;

    @Schema(description = "Prénom de l'utilisateur (transmis lors du premier login Apple uniquement)", example = "Jean")
    private String firstName;

    @Schema(description = "Nom de famille de l'utilisateur (transmis lors du premier login Apple uniquement)", example = "Dupont")
    private String lastName;
}
