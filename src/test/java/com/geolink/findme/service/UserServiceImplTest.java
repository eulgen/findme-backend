package com.geolink.findme.service;

import com.geolink.findme.dto.mapper.UserMapper;
import com.geolink.findme.dto.request.UpdateProfileRequestDTO;
import com.geolink.findme.dto.response.UserProfileDTO;
import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.UserNotFoundException;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.service.userService.UserServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

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
    void devrait_lever_une_exception_lorsque_l_utilisateur_n_existe_pas() {
        // Given
        when(userRepository.findByEmail("unknown@geolink.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getProfile("unknown@geolink.com"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("unknown@geolink.com");

        verify(userRepository, times(1)).findByEmail("unknown@geolink.com");
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
        assertThat(testUser.getFullName()).isEqualTo("Pierre");
        assertThat(testUser.getFullName()).isEqualTo("Martin");
        assertThat(testUser.getPhoneNumber()).isEqualTo("+33612345678");
        verify(userRepository, times(1)).findByEmail("test@geolink.com");
        verify(userRepository, times(1)).save(testUser);
    }
}