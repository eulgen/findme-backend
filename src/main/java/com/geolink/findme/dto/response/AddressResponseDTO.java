package com.geolink.findme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * DTO de réponse contenant les détails complets d'une adresse.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponseDTO {

    private Long id;
    private String addressCode;
    private String country;
    private String city;
    private String street;
    private String houseNumber;
    private String photoUrl;
    private GpsCoordinateDTO gps;
    private Instant createdAt;
    private Instant updatedAt;
}
