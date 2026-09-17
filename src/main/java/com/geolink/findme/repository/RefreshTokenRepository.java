package com.geolink.findme.repository;

import com.geolink.findme.entity.RefreshToken;
import com.geolink.findme.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository Spring Data JPA pour la gestion des refresh tokens ({@link RefreshToken}).
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Recherche un refresh token par son hash SHA-256.
     *
     * @param tokenHash le hash du token
     * @return un {@link Optional} contenant le token s'il existe
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Révoque tous les refresh tokens actifs d'un utilisateur (déconnexion ou réinitialisation).
     *
     * @param user l'utilisateur concerné
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user AND r.revoked = false")
    void revokeAllByUser(@Param("user") User user);
}
