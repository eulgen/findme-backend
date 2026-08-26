package com.geolink.findme.repository;

import com.geolink.findme.entity.GpsCoordinate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository Spring Data JPA pour l'entité GpsCoordinate.
 */
@Repository
public interface GpsCoordinateRepository extends JpaRepository<GpsCoordinate, Long> {
}
