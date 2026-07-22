package com.example.findme.service;

import com.example.findme.dto.request.SigninRequest;
import com.example.findme.dto.request.SignupRequest;
import com.example.findme.dto.response.AuthResponse;
import com.example.findme.entity.User;
import com.example.findme.enums.Role;
import com.example.findme.exception.DuplicateResourceException;
import com.example.findme.exception.ResourceNotFoundException;
import com.example.findme.repository.UserRepository;
import com.example.findme.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.findme.security.JwtService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de la logique metier {@link AuthServiceImpl}.
 * Utilise Mockito pour simuler le comportement du Repository
 * et isoler le service completement de la base de donnees.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    // =========================================================
    // Tests : signup
    // =========================================================

    @Test
    @DisplayName("signup() doit inscrire l'utilisateur avec succes")
    void signup_shouldCreateUser_whenDataIsValid() {
        // Arrange
        SignupRequest request = SignupRequest.builder()
                .email("test@findme.com")
                .username("testuser")
                .password("password123")
                .build();

        User savedUser = User.builder()
                .id(1L)
                .email("test@findme.com")
                .username("testuser")
                .password("encoded_password")
                .role(Role.UTILISATEUR)
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("vrai_token_jwt");

        // Act
        AuthResponse response = authService.signup(request);

        // Assert
        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().email()).isEqualTo("test@findme.com");
        assertThat(response.token()).isEqualTo("vrai_token_jwt");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("signup() doit lever une exception si l'email existe deja")
    void signup_shouldThrowException_whenEmailAlreadyExists() {
        // Arrange
        SignupRequest request = SignupRequest.builder()
                .email("duplicate@findme.com")
                .username("testuser")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("duplicate@findme.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email est deja utilisee");

        verify(userRepository, never()).save(any(User.class));
    }

    // =========================================================
    // Tests : signin
    // =========================================================

    @Test
    @DisplayName("signin() doit authentifier l'utilisateur avec succes")
    void signin_shouldAuthenticateUser_whenCredentialsAreValid() {
        // Arrange
        SigninRequest request = SigninRequest.builder()
                .email("test@findme.com")
                .password("password123")
                .build();

        User user = User.builder()
                .id(1L)
                .email("test@findme.com")
                .password("encoded_password")
                .role(Role.UTILISATEUR)
                .build();

        when(userRepository.findByEmail("test@findme.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtService.generateToken(any(User.class))).thenReturn("vrai_token_jwt");

        // Act
        AuthResponse response = authService.signin(request);

        // Assert
        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().email()).isEqualTo("test@findme.com");
        assertThat(response.token()).isEqualTo("vrai_token_jwt");
    }

    @Test
    @DisplayName("signin() doit lever une exception si le mot de passe est faux")
    void signin_shouldThrowException_whenPasswordIsWrong() {
        // Arrange
        SigninRequest request = SigninRequest.builder()
                .email("test@findme.com")
                .password("wrongpassword")
                .build();

        User user = User.builder()
                .email("test@findme.com")
                .password("encoded_password")
                .build();

        when(userRepository.findByEmail("test@findme.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encoded_password")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.signin(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Identifiants incorrects");
    }
}
