package com.geolink.findme.dto.mapper;

import com.geolink.findme.dto.response.SupportResponseDTO;
import com.geolink.findme.entity.SupportMessage;
import org.springframework.stereotype.Component;

/**
 * Mapper composant Spring assurant la conversion entre SupportMessage et SupportResponseDTO.
 */
@Component
public class SupportMessageMapper {

    public SupportResponseDTO toDTO(SupportMessage entity) {
        if (entity == null) {
            return null;
        }

        Long userId = entity.getUser() != null ? entity.getUser().getId() : null;
        String userFullName = entity.getUser() != null ? entity.getUser().getFullName() : null;

        return SupportResponseDTO.builder()
                .id(entity.getId())
                .userId(userId)
                .userFullName(userFullName)
                .name(entity.getName())
                .email(entity.getEmail())
                .message(entity.getMessage())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
