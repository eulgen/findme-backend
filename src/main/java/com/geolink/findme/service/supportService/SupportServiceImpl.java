package com.geolink.findme.service.supportService;

import com.geolink.findme.dto.mapper.SupportMessageMapper;
import com.geolink.findme.dto.request.CreateSupportRequestDTO;
import com.geolink.findme.dto.request.UpdateSupportStatusRequestDTO;
import com.geolink.findme.dto.response.SupportResponseDTO;
import com.geolink.findme.entity.SupportMessage;
import com.geolink.findme.entity.SupportStatus;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.SupportMessageNotFoundException;
import com.geolink.findme.repository.SupportMessageRepository;
import com.geolink.findme.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implémentation du service métier de support client.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupportServiceImpl implements SupportService {

    private final SupportMessageRepository supportMessageRepository;
    private final UserRepository userRepository;
    private final SupportMessageMapper supportMessageMapper;

    @Override
    @Transactional
    public SupportResponseDTO createSupportMessage(CreateSupportRequestDTO dto, String authenticatedEmail) {
        String targetEmail = (authenticatedEmail != null && !authenticatedEmail.isBlank())
                ? authenticatedEmail
                : dto.getEmail();

        Optional<User> userOptional = userRepository.findByEmail(targetEmail);
        if (userOptional.isEmpty() && dto.getEmail() != null) {
            userOptional = userRepository.findByEmail(dto.getEmail());
        }

        SupportMessage supportMessage = SupportMessage.builder()
                .user(userOptional.orElse(null))
                .name(dto.getName())
                .email(dto.getEmail())
                .message(dto.getMessage())
                .status(SupportStatus.PENDING)
                .build();

        SupportMessage savedMessage = supportMessageRepository.save(supportMessage);
        log.info("Nouveau message de support créé avec l'ID {} pour l'email {}", savedMessage.getId(), dto.getEmail());

        return supportMessageMapper.toDTO(savedMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportResponseDTO> getSupportMessages(SupportStatus status, Pageable pageable) {
        Page<SupportMessage> page;
        if (status != null) {
            page = supportMessageRepository.findByStatus(status, pageable);
        } else {
            page = supportMessageRepository.findAll(pageable);
        }
        return page.map(supportMessageMapper::toDTO);
    }

    @Override
    @Transactional
    public SupportResponseDTO updateSupportStatus(Long id, UpdateSupportStatusRequestDTO dto) {
        SupportMessage message = supportMessageRepository.findById(id)
                .orElseThrow(() -> new SupportMessageNotFoundException("Message de support non trouvé avec l'identifiant : " + id));

        message.setStatus(dto.getStatus());
        SupportMessage updatedMessage = supportMessageRepository.save(message);
        log.info("Statut du message de support ID {} mis à jour vers {}", id, dto.getStatus());

        return supportMessageMapper.toDTO(updatedMessage);
    }
}
