package com.geolink.findme.authservice.repository;

import com.geolink.findme.authservice.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository Spring Data JPA pour la gestion des permissions ({@link Permission}).
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * Recherche une permission par son code unique (ex. {@code USER_LIST_VIEW}).
     *
     * @param code le code de la permission
     * @return un {@link Optional} contenant la permission si elle existe
     */
    Optional<Permission> findByCode(String code);
}
