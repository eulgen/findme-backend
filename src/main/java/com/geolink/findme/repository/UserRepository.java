package com.geolink.findme.repository;

import com.geolink.findme.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository Spring Data JPA pour la gestion des utilisateurs ({@link User}).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

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

    /**
     * Recherche paginée des utilisateurs filtrée par nom ou email.
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL OR TRIM(CAST(:search AS string)) = '' OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);
}
