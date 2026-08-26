package com.geolink.findme.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Requête pour la mise à jour du rôle d'un utilisateur")
public class RoleUpdateDTO {

    @NotBlank(message = "Le nom du rôle est obligatoire")
    @Schema(description = "Nouveau rôle (ex: ADMIN, USER, SUPPORT_AGENT)", example = "ADMIN")
    private String roleName;
}
