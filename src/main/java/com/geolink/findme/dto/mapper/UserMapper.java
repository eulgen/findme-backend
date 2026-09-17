package com.geolink.findme.dto.mapper;

import com.geolink.findme.dto.response.UserProfileDTO;
import com.geolink.findme.entity.User;
import com.geolink.findme.service.storageService.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Composant de mapping pour convertir les entités {@link User} en {@link UserProfileDTO}.
 */
@Component
public class UserMapper {

    private final StorageService storageService;

    public UserMapper() {
        this.storageService = null;
    }

    @Autowired
    public UserMapper(@Autowired(required = false) StorageService storageService) {
        this.storageService = storageService;
    }

    public UserProfileDTO toDto(User user) {
        if (user == null) {
            return null;
        }

        String profileImageUrl = null;
        if (user.getProfileImage() != null && !user.getProfileImage().isBlank()) {
            profileImageUrl = storageService != null
                    ? storageService.getPublicUrl(user.getProfileImage(), "profiles")
                    : user.getProfileImage();
        }

        return UserProfileDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .profileImage(profileImageUrl)
                .role(user.primaryRole())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
