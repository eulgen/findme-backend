package com.example.findme.entity;

import com.example.findme.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entite JPA representant un utilisateur de l'application FindMe.
 *
 * <p>Mappe sur la table {@code users} en base de donnees MySQL.
 * Un utilisateur peut s'inscrire via email/mot de passe ou via OAuth
 * (Google, iCloud). Il possede un role ({@link Role}) determinant
 * ses droits d'acces et peut avoir jusqu'a 4 adresses associees.</p>
 *
 * <p>Le mot de passe est toujours stocke sous forme hachee (BCrypt)
 * et ne doit jamais etre expose dans les reponses de l'API.</p>
 *
 * <p>Conventions Lombok utilisees :</p>
 * <ul>
 *   <li>{@code @Data} : genere getters, setters, equals, hashCode, toString.</li>
 *   <li>{@code @Builder} : active le pattern Builder pour la construction.</li>
 *   <li>{@code @NoArgsConstructor} / {@code @AllArgsConstructor} : constructeurs.</li>
 * </ul>
 *
 * @author findme-team
 * @version 1.0.0
 * @see com.example.findme.enums.Role
 * @see com.example.findme.repository.UserRepository
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email",    columnNames = "email"),
        @UniqueConstraint(name = "uk_users_username", columnNames = "username")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Identifiant unique de l'utilisateur, genere automatiquement
     * par la base de donnees (auto-increment).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Adresse email de l'utilisateur.
     * Doit etre unique en base de donnees.
     * Utilisee comme identifiant de connexion.
     */
    @Column(name = "email", nullable = false, length = 255)
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit etre une adresse valide")
    @Size(max = 255, message = "L'email ne peut pas depasser 255 caracteres")
    private String email;

    /**
     * Nom d'utilisateur unique visible publiquement.
     * Doit etre unique en base de donnees.
     */
    @Column(name = "username", nullable = false, length = 100)
    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 100, message = "Le nom d'utilisateur doit contenir entre 3 et 100 caracteres")
    private String username;

    /**
     * Mot de passe de l'utilisateur, toujours stocke hache en BCrypt.
     * Ce champ ne doit JAMAIS etre inclus dans les reponses de l'API.
     * Peut etre null pour les utilisateurs connectes via OAuth uniquement.
     */
    @Column(name = "password", length = 255)
    private String password;

    /**
     * Role de l'utilisateur determinant ses droits d'acces.
     * Valeur par defaut : {@link Role#UTILISATEUR}.
     * Stocke comme chaine de caracteres en base de donnees.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.UTILISATEUR;

    /**
     * URL de la photo de profil de l'utilisateur.
     * Champ optionnel, peut etre null ou vide.
     */
    @Column(name = "photo", length = 500)
    private String photo;

    /**
     * Numero de telephone de l'utilisateur.
     * Champ optionnel au format international (ex: +237600000000).
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Date et heure de creation du compte, renseignee automatiquement
     * par Hibernate lors de la premiere persistance. Non modifiable.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Date et heure de la derniere modification du compte,
     * mise a jour automatiquement par Hibernate a chaque modification.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
