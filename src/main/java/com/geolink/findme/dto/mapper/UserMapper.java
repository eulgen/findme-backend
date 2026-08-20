package com.geolink.findme.dto.mapper;

import com.geolink.findme.dto.response.UserProfileDTO;
import com.geolink.findme.entity.User;
import org.springframework.stereotype.Component;

/**
 * Composant de mapping pour convertir les entités {@link User} en {@link UserProfileDTO}.
 */
@Component
public class UserMapper {

    public UserProfileDTO toDto(User user) {
        if (user == null) {
            return null;
        }

        return UserProfileDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.primaryRole())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
