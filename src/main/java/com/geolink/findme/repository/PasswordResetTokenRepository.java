package com.geolink.findme.repository;

import com.geolink.findme.entity.PasswordResetToken;
import com.geolink.findme.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository Spring Data JPA pour la gestion des tokens de réinitialisation de mot de passe ({@link PasswordResetToken}).
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Recherche un token de réinitialisation par son hash SHA-256.
     *
     * @param tokenHash le hash du token
     * @return un {@link Optional} contenant le token s'il existe
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Marque tous les anciens tokens de réinitialisation d'un utilisateur comme utilisés (invalides).
     *
     * @param user l'utilisateur concerné
     */
    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.used = true WHERE p.user = :user AND p.used = false")
    void markAllUsedByUser(@Param("user") User user);
}
