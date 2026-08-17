package com.geolink.findme.authservice.repository;

import com.geolink.findme.authservice.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository Spring Data JPA pour la gestion des rôles ({@link Role}).
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Recherche un rôle par son nom (ex. {@code USER}, {@code ADMIN}).
     *
     * @param name le nom du rôle
     * @return un {@link Optional} contenant le rôle s'il existe
     */
    Optional<Role> findByName(String name);
}
