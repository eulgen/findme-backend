package com.geolink.findme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * DTO de réponse représentant le profil public d'un utilisateur.
 * Le champ {@code role} est le rôle principal calculé (ex. USER, ADMIN).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String profileImage;
    private String role;
    private String status;
    private Instant createdAt;
    private Instant lastLoginAt;
}
