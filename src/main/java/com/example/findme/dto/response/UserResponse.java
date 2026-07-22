package com.example.findme.dto.response;

import com.example.findme.enums.Role;
import lombok.Builder;

/**
 * DTO (Data Transfer Object) representant la reponse publique d'un utilisateur.
 *
 * <p>Utilise comme un {@code Record} Java 14+ pour une immuabilite parfaite.
 * Exclut expressement les champs sensibles comme {@code password}.</p>
 *
 * @param id l'identifiant technique
 * @param email l'email de l'utilisateur
 * @param username son nom d'utilisateur
 * @param role son role dans l'application
 * @param photo l'URL de sa photo de profil
 * @param phoneNumber son numero de telephone
 */
@Builder
public record UserResponse(
        Long id,
        String email,
        String username,
        Role role,
        String photo,
        String phoneNumber
) {
}
