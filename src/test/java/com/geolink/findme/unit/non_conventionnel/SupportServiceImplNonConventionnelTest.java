package com.geolink.findme.unit.non_conventionnel;

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

class SupportServiceImplNonConventionnelTest {


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
    @DisplayName("Devrait lever SupportMessageNotFoundException si le message de support est introuvable")
    void devrait_lever_exception_si_message_de_support_introuvable() {
        // Given
        when(supportMessageRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateSupportStatusRequestDTO updateDTO = UpdateSupportStatusRequestDTO.builder()
                .status(SupportStatus.PROCESSED)
                .build();

        // When & Then
        assertThatThrownBy(() -> supportService.updateSupportStatus(99L, updateDTO))
                .isInstanceOf(SupportMessageNotFoundException.class)
                .hasMessageContaining("99");

        verify(supportMessageRepository, never()).save(any(SupportMessage.class));
    }
}
