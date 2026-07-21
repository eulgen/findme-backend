package com.example.findme.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) utilise pour encapsuler les donnees
 * d'inscription d'un nouvel utilisateur.
 *
 * <p>Assure la validation stricte des champs avant que la requete
 * n'atteigne le controleur (grace a {@code @Valid}).</p>
 *
 * @author findme-team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(message = "Le format de l'adresse email est invalide")
    @Size(max = 255, message = "L'adresse email ne doit pas depasser 255 caracteres")
    private String email;

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 100, message = "Le nom d'utilisateur doit contenir entre 3 et 100 caracteres")
    private String username;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, max = 50, message = "Le mot de passe doit contenir entre 6 et 50 caracteres")
    private String password;

    // Optionnel a l'inscription
    @Size(max = 20, message = "Le numero de telephone ne doit pas depasser 20 caracteres")
    private String phoneNumber;
}
