package com.geolink.findme.unit.conventionnel;

import com.geolink.findme.dto.mapper.UserMapper;
import com.geolink.findme.service.userService.UserServiceImpl;
import com.geolink.findme.dto.request.UpdateProfileRequestDTO;
import com.geolink.findme.dto.response.UserProfileDTO;
import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.UserNotFoundException;
import com.geolink.findme.repository.UserRepository;

import com.geolink.findme.service.storageService.StorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

class UserServiceImplConventionnelTest {


    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@geolink.com")
                .fullName("Jean Dupont")
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void devrait_retourner_le_profil_lorsque_l_utilisateur_existe() {
        // Given
        when(userRepository.findByEmail("test@geolink.com")).thenReturn(Optional.of(testUser));

        // When
        UserProfileDTO result = userService.getProfile("test@geolink.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@geolink.com");
        verify(userRepository, times(1)).findByEmail("test@geolink.com");
    }

    @Test
    void devrait_mettre_a_jour_le_profil_utilisateur_avec_succes() {
        // Given
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .fullName("Pierre Martin")
                .phoneNumber("+33612345678")
                .build();

        when(userRepository.findByEmail("test@geolink.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserProfileDTO result = userService.updateProfile("test@geolink.com", request);

        // Then
        assertThat(result).isNotNull();
        assertThat(testUser.getFullName()).isEqualTo("Pierre Martin");
        assertThat(testUser.getPhoneNumber()).isEqualTo("+33612345678");
        verify(userRepository, times(1)).findByEmail("test@geolink.com");
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void devrait_uploader_l_image_de_profil_avec_succes() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                "content".getBytes()
        );
        when(userRepository.findByEmail("test@geolink.com")).thenReturn(Optional.of(testUser));
        when(storageService.store(file, "profiles")).thenReturn("generated-uuid.jpg");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserProfileDTO result = userService.uploadProfileImage("test@geolink.com", file);

        // Then
        assertThat(result).isNotNull();
        assertThat(testUser.getProfileImage()).isEqualTo("generated-uuid.jpg");
        verify(storageService, times(1)).store(file, "profiles");
        verify(userRepository, times(1)).save(testUser);
    }
}
