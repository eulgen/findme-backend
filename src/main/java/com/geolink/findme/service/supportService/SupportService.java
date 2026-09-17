package com.geolink.findme.service.supportService;

import com.geolink.findme.dto.request.CreateSupportRequestDTO;
import com.geolink.findme.dto.request.UpdateSupportStatusRequestDTO;
import com.geolink.findme.dto.response.SupportResponseDTO;
import com.geolink.findme.entity.SupportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface du service métier pour la gestion du support client.
 */
public interface SupportService {

    /**
     * Soumission d'un nouveau message de support client.
     * Associe l'utilisateur s'il existe par son email ou via le principal connecté.
     */
    SupportResponseDTO createSupportMessage(CreateSupportRequestDTO dto, String authenticatedEmail);

    /**
     * Récupération de la liste paginée des messages de support, avec filtrage optionnel par statut.
     */
    Page<SupportResponseDTO> getSupportMessages(SupportStatus status, Pageable pageable);

    /**
     * Mise à jour du statut d'un message de support (ex: PENDING -> PROCESSED).
     */
    SupportResponseDTO updateSupportStatus(Long id, UpdateSupportStatusRequestDTO dto);
}
