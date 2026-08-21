package com.geolink.findme.repository;

import com.geolink.findme.entity.SupportMessage;
import com.geolink.findme.entity.SupportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository Spring Data JPA pour la gestion de la persistance des messages de support client.
 */
@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long>, JpaSpecificationExecutor<SupportMessage> {

    /**
     * Recherche paginée des messages de support selon leur statut (PENDING / PROCESSED).
     */
    Page<SupportMessage> findByStatus(SupportStatus status, Pageable pageable);

    /**
     * Récupère tous les messages de support rédigés par un utilisateur spécifique (par son ID).
     */
    List<SupportMessage> findByUserId(Long userId);

    /**
     * Récupère tous les messages de support associés à une adresse email.
     */
    List<SupportMessage> findByEmailIgnoreCase(String email);
}
