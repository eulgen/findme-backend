package com.example.findme.repository;

import com.example.findme.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Interface d'acces aux donnees pour l'entite {@link Place}.
 *
 * @author findme-team
 */
@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {

    /**
     * Recupere tous les lieux associes a l'ID d'un utilisateur.
     * Utile pour l'endpoint GET /api/v1/places.
     *
     * @param userId l'identifiant de l'utilisateur
     * @return une liste de lieux
     */
    List<Place> findByUserId(Long userId);

    /**
     * Recupere un lieu specifique s'il appartient a un utilisateur donne.
     * Utile pour securiser les actions (GET, PUT, DELETE un lieu precis).
     *
     * @param id l'identifiant du lieu
     * @param userId l'identifiant du proprietaire
     * @return le lieu (s'il existe et appartient a l'utilisateur)
     */
    Optional<Place> findByIdAndUserId(Long id, Long userId);
}
