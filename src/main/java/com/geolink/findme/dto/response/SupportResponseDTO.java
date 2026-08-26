package com.geolink.findme.dto.response;

import com.geolink.findme.entity.SupportStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * DTO de réponse contenant les informations d'un message de support.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportResponseDTO {

    private Long id;
    private Long userId;
    private String userFullName;
    private String name;
    private String email;
    private String message;
    private SupportStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
