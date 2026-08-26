package com.geolink.findme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO encapsulant les informations utilisateur incluses dans le JSON d'export PDF.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPdfExportDTO {

    private String fullName;
    private String email;
    private String phoneNumber;
}
