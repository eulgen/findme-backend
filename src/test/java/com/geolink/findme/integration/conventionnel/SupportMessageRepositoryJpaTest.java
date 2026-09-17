package com.geolink.findme.integration.conventionnel;

import com.geolink.findme.entity.SupportMessage;
import com.geolink.findme.entity.SupportStatus;
import com.geolink.findme.repository.SupportMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration JPA du repository {@link SupportMessageRepository}.
 * Vérifie la recherche paginée par statut et par email.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testsupportdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Tests d'intégration JPA - SupportMessageRepository")

class SupportMessageRepositoryJpaTest {


    @Autowired
    private SupportMessageRepository supportMessageRepository;

    private SupportMessage msg1;
    private SupportMessage msg2;

    @BeforeEach
    void setUp() {
        supportMessageRepository.deleteAll();

        msg1 = SupportMessage.builder()
                .name("Client Un")
                .email("client1@geolink.com")
                .message("Impossible de valider mon adresse")
                .status(SupportStatus.PENDING)
                .build();

        msg2 = SupportMessage.builder()
                .name("Client Deux")
                .email("client2@geolink.com")
                .message("Quels sont les tarifs ?")
                .status(SupportStatus.PROCESSED)
                .build();

        supportMessageRepository.save(msg1);
        supportMessageRepository.save(msg2);
    }

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Des messages avec différents statuts (`PENDING` et `PROCESSED`).
     * 2. When   : Recherche des messages au statut `PENDING`.
     * 3. Then   : La page doit contenir uniquement le premier message.
     */

    /**
     * LOGIQUE DU TEST :
     * 1. Given  : Un message associé à l'email "client1@geolink.com".
     * 2. When   : Recherche par email sans tenir compte de la casse (`findByEmailIgnoreCase`).
     * 3. Then   : Le message doit être trouvé.
     */

    @Test
    @DisplayName("Devrait trouver les messages de support par statut avec pagination")
    void devrait_trouver_messages_support_par_statut() {
        Page<SupportMessage> pendingMessages = supportMessageRepository.findByStatus(SupportStatus.PENDING, PageRequest.of(0, 10));

        assertThat(pendingMessages.getContent()).hasSize(1);
        assertThat(pendingMessages.getContent().get(0).getEmail()).isEqualTo("client1@geolink.com");
    }

    @Test
    @DisplayName("Devrait trouver les messages de support par email de manière insensible à la casse")
    void devrait_trouver_messages_support_par_email_insensible_casse() {
        List<SupportMessage> list = supportMessageRepository.findByEmailIgnoreCase("CLIENT1@GEOLINK.COM");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getMessage()).isEqualTo("Impossible de valider mon adresse");
    }
}
