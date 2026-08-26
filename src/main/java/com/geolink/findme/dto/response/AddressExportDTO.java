package com.geolink.findme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * DTO complet de réponse pour l'endpoint GET /api/addresses/{id}/export destiné au rendu PDF frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressExportDTO {

    private Long addressId;
    private String addressCode;
    private UserPdfExportDTO user;
    private String formattedAddress;
    private String country;
    private String city;
    private String street;
    private String houseNumber;
    private GpsCoordinateDTO gps;
    private String photoUrl;
    private Instant generatedAt;
}
