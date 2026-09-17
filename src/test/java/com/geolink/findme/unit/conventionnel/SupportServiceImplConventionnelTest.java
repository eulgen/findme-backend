package com.geolink.findme.unit.conventionnel;

import com.geolink.findme.dto.mapper.SupportMessageMapper;
import com.geolink.findme.service.supportService.SupportServiceImpl;
import com.geolink.findme.dto.request.CreateSupportRequestDTO;
import com.geolink.findme.dto.request.UpdateSupportStatusRequestDTO;
import com.geolink.findme.dto.response.SupportResponseDTO;
import com.geolink.findme.entity.SupportMessage;
import com.geolink.findme.entity.SupportStatus;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.SupportMessageNotFoundException;
import com.geolink.findme.repository.SupportMessageRepository;
import com.geolink.findme.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du service de support client {@link SupportServiceImpl}.
 * Vérifie la création de messages de support (utilisateurs connectés et anonymes), la pagination et la mise à jour des statuts.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires du service SupportServiceImpl")

class SupportServiceImplConventionnelTest {


    @Mock
    private SupportMessageRepository supportMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private SupportMessageMapper supportMessageMapper = new SupportMessageMapper();

    @InjectMocks
    private SupportServiceImpl supportService;

    private User testUser;
    private SupportMessage testMessage;
    private CreateSupportRequestDTO createRequestDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@geolink.com");
        testUser.setFullName("User Test");

        testMessage = SupportMessage.builder()
                .id(100L)
                .name("Jean Dupont")
                .email("user@geolink.com")
                .message("Problème de connexion")
                .status(SupportStatus.PENDING)
                .user(testUser)
                .build();

        createRequestDTO = CreateSupportRequestDTO.builder()
                .name("Jean Dupont")
                .email("user@geolink.com")
                .message("Problème de connexion")
                .build();
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Un utilisateur connecté avec l'email "user@geolink.com" existe en BDD.
     * 2. When   : createSupportMessage est appelée avec les données du formulaire et l'email de session.
     * 3. Then   : Le message est associé à l'entité User, le statut est initialisé à PENDING et le DTO retourné est conforme.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : L'email de la demande de support "anonymous@geolink.com" n'existe dans aucun compte utilisateur.
     * 2. When   : createSupportMessage est appelée sans session d'authentification (null).
     * 3. Then   : Le message de support est créé sans lien utilisateur (user = null) avec le statut PENDING.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Le paramètre status est null.
     * 2. When   : getSupportMessages(null, pageable) est appelée.
     * 3. Then   : Le repository appelle findAll(pageable) et retourne la liste complète des messages paginés.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Le filtre status = PENDING est spécifié.
     * 2. When   : getSupportMessages(PENDING, pageable) est appelée.
     * 3. Then   : Le repository filtre les messages par le statut PENDING via findByStatus.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Le message de support ID=100L existe avec le statut PENDING.
     * 2. When   : updateSupportStatus(100L, PROCESSED) est exécutée.
     * 3. Then   : Le statut du message devient PROCESSED et les modifications sont enregistrées en BDD.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Le message de support ID=99L n'existe pas.
     * 2. When   : updateSupportStatus(99L, PROCESSED) est appelée.
     * 3. Then   : La méthode lève SupportMessageNotFoundException sans procéder à la sauvegarde.
     */

    @Test
    @DisplayName("Devrait créer un message de support associé à un utilisateur connecté")
    void devrait_creer_message_support_pour_utilisateur_connecte() {
        // Given
        when(userRepository.findByEmail("user@geolink.com")).thenReturn(Optional.of(testUser));
        when(supportMessageRepository.save(any(SupportMessage.class))).thenReturn(testMessage);

        // When
        SupportResponseDTO response = supportService.createSupportMessage(createRequestDTO, "user@geolink.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(SupportStatus.PENDING);
        verify(supportMessageRepository, times(1)).save(any(SupportMessage.class));
    }

    @Test
    @DisplayName("Devrait créer un message de support pour un utilisateur non identifié")
    void devrait_creer_message_support_pour_utilisateur_anonyme() {
        // Given
        CreateSupportRequestDTO anonRequest = CreateSupportRequestDTO.builder()
                .name("Visiteur")
                .email("anonymous@geolink.com")
                .message("Question sur l'application")
                .build();

        SupportMessage anonMessage = SupportMessage.builder()
                .id(101L)
                .name("Visiteur")
                .email("anonymous@geolink.com")
                .message("Question sur l'application")
                .status(SupportStatus.PENDING)
                .user(null)
                .build();

        when(userRepository.findByEmail("anonymous@geolink.com")).thenReturn(Optional.empty());
        when(supportMessageRepository.save(any(SupportMessage.class))).thenReturn(anonMessage);

        // When
        SupportResponseDTO response = supportService.createSupportMessage(anonRequest, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getUserId()).isNull();
        verify(supportMessageRepository, times(1)).save(any(SupportMessage.class));
    }

    @Test
    @DisplayName("Devrait retourner tous les messages de support paginés en l'absence de filtre de statut")
    void devrait_retourner_les_messages_de_support_paginees_sans_filtre() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<SupportMessage> page = new PageImpl<>(List.of(testMessage));
        when(supportMessageRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<SupportResponseDTO> result = supportService.getSupportMessages(null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(supportMessageRepository, times(1)).findAll(pageable);
        verify(supportMessageRepository, never()).findByStatus(any(), any());
    }

    @Test
    @DisplayName("Devrait filtrer les messages de support par leur statut")
    void devrait_retourner_les_messages_de_support_paginees_filtrees_par_statut() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<SupportMessage> page = new PageImpl<>(List.of(testMessage));
        when(supportMessageRepository.findByStatus(SupportStatus.PENDING, pageable)).thenReturn(page);

        // When
        Page<SupportResponseDTO> result = supportService.getSupportMessages(SupportStatus.PENDING, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(supportMessageRepository, times(1)).findByStatus(SupportStatus.PENDING, pageable);
    }

    @Test
    @DisplayName("Devrait mettre à jour le statut d'un message de support avec succès")
    void devrait_mettre_a_jour_le_statut_d_un_message_de_support() {
        // Given
        when(supportMessageRepository.findById(100L)).thenReturn(Optional.of(testMessage));
        when(supportMessageRepository.save(any(SupportMessage.class))).thenReturn(testMessage);

        UpdateSupportStatusRequestDTO updateDTO = UpdateSupportStatusRequestDTO.builder()
                .status(SupportStatus.PROCESSED)
                .build();

        // When
        SupportResponseDTO response = supportService.updateSupportStatus(100L, updateDTO);

        // Then
        assertThat(response).isNotNull();
        assertThat(testMessage.getStatus()).isEqualTo(SupportStatus.PROCESSED);
        verify(supportMessageRepository, times(1)).save(testMessage);
    }
}
