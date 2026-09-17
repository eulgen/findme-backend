package com.geolink.findme.unit.non_conventionnel;

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

class UserServiceImplNonConventionnelTest {


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
    void devrait_lever_une_exception_lorsque_l_utilisateur_n_existe_pas() {
        // Given
        when(userRepository.findByEmail("unknown@geolink.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getProfile("unknown@geolink.com"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("unknown@geolink.com");

        verify(userRepository, times(1)).findByEmail("unknown@geolink.com");
    }
}
