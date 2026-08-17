package com.geolink.findme.authservice.repository;

import com.geolink.findme.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository Spring Data JPA pour la gestion des utilisateurs ({@link User}).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Recherche un utilisateur par son adresse email.
     *
     * @param email l'adresse email
     * @return un {@link Optional} contenant l'utilisateur s'il existe
     */
    Optional<User> findByEmail(String email);

    /**
     * Vérifie si un utilisateur existe déjà avec cette adresse email.
     *
     * @param email l'adresse email
     * @return {@code true} si l'email existe déjà
     */
    boolean existsByEmail(String email);
}
