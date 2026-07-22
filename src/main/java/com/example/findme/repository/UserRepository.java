package com.example.findme.repository;

import com.example.findme.entity.User;
import com.example.findme.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entite {@link User}.
 *
 * <p>Etend {@link JpaRepository} pour beneficier de toutes les
 * operations CRUD standard (save, findById, findAll, delete, etc.)
 * sans implementer la moindre ligne de code SQL.</p>
 *
 * <p>Les methodes de requete derivees suivent la convention de nommage
 * Spring Data JPA : {@code findBy[Champ]}, {@code existsBy[Champ]}, etc.</p>
 *
 * <p>Principe SOLID applique :</p>
 * <ul>
 *   <li><strong>I (Interface Segregation)</strong> : seules les methodes
 *       reellement utilisees par les services sont declarees.</li>
 *   <li><strong>D (Dependency Inversion)</strong> : les services dependent
 *       de cette interface, pas d'une implementation concrete.</li>
 * </ul>
 *
 * @author findme-team
 * @version 1.0.0
 * @see User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Recherche un utilisateur par son adresse email.
     * Utilise pour l'authentification (signin) et la validation de l'unicite.
     *
     * @param email l'adresse email de l'utilisateur a rechercher
     * @return un {@link Optional} contenant l'utilisateur si trouve, vide sinon
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifie si un utilisateur existe avec l'adresse email donnee.
     * Utilise lors de l'inscription pour detecter les emails deja utilises.
     *
     * @param email l'adresse email a verifier
     * @return {@code true} si un utilisateur avec cet email existe deja
     */
    boolean existsByEmail(String email);

    /**
     * Verifie si un utilisateur existe avec le nom d'utilisateur donne.
     * Utilise lors de l'inscription pour detecter les usernames deja pris.
     *
     * @param username le nom d'utilisateur a verifier
     * @return {@code true} si un utilisateur avec ce username existe deja
     */
    boolean existsByUsername(String username);

    /**
     * Recherche tous les utilisateurs ayant un role specifique, avec pagination.
     * Utilise par les endpoints d'administration pour lister les utilisateurs
     * par role (ex: tous les admins, tous les utilisateurs standard).
     *
     * @param role     le role a filtrer ({@link Role#UTILISATEUR} ou {@link Role#ADMIN})
     * @param pageable les parametres de pagination et de tri
     * @return une page d'utilisateurs correspondant au role donne
     */
    Page<User> findByRole(Role role, Pageable pageable);
}
