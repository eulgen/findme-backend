package com.geolink.findme.repository;

import com.geolink.findme.entity.Address;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository Spring Data JPA pour la gestion des adresses et des recherches filtrées.
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, Long>, JpaSpecificationExecutor<Address> {

    /**
     * Compte le nombre d'adresses rattachées à un utilisateur.
     */
    long countByUsers_Id(Long userId);

    /**
     * Vérifie si un code d'adresse existe déjà.
     */
    boolean existsByAddressCode(String addressCode);

    /**
     * Recherche une adresse par son ID et l'ID de l'utilisateur propriétaire.
     */
    @Query("SELECT a FROM Address a JOIN a.users u WHERE a.id = :addressId AND u.id = :userId")
    Optional<Address> findByIdAndUserId(@Param("addressId") Long addressId, @Param("userId") Long userId);

    /**
     * Recherche paginée des adresses associées à un utilisateur donné.
     */
    @Query("SELECT a FROM Address a JOIN a.users u WHERE u.id = :userId")
    Page<Address> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Recherche paginée d'adresses filtrable par pays, ville et quartier (pour les administrateurs).
     */
    @Query("SELECT a FROM Address a WHERE " +
           "(:country IS NULL OR TRIM(CAST(:country AS string)) = '' OR LOWER(a.country) LIKE LOWER(CONCAT('%', CAST(:country AS string), '%'))) AND " +
           "(:city IS NULL OR TRIM(CAST(:city AS string)) = '' OR LOWER(a.city) LIKE LOWER(CONCAT('%', CAST(:city AS string), '%'))) AND " +
           "(:district IS NULL OR TRIM(CAST(:district AS string)) = '' OR LOWER(a.district) LIKE LOWER(CONCAT('%', CAST(:district AS string), '%')))")
    Page<Address> searchAddresses(
            @Param("country") String country,
            @Param("city") String city,
            @Param("district") String district,
            Pageable pageable
    );
}
