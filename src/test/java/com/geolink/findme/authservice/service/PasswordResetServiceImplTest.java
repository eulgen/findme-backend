package com.geolink.findme.authservice.service;

import com.geolink.findme.authservice.entity.PasswordResetToken;
import com.geolink.findme.authservice.entity.User;
import com.geolink.findme.authservice.exception.InvalidOrExpiredTokenException;
import com.geolink.findme.authservice.repository.PasswordResetTokenRepository;
import com.geolink.findme.authservice.repository.UserRepository;
import com.geolink.findme.authservice.security.JwtService;
import com.geolink.findme.authservice.service.passwordService.PasswordResetServiceImpl;
import com.geolink.findme.authservice.service.passwordService.RefreshTokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("test@geolink.com").passwordHash("old_pwd").build();
    }

    @Test
    void devrait_enregistrer_un_token_lors_de_la_demande_si_l_utilisateur_existe() {
        // Given
        when(userRepository.findByEmail("test@geolink.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateOpaqueToken()).thenReturn("raw_token");
        when(jwtService.hashToken("raw_token")).thenReturn("hashed_token");

        // When
        passwordResetService.requestReset("test@geolink.com");

        // Then
        verify(userRepository, times(1)).findByEmail("test@geolink.com");
        verify(passwordResetTokenRepository, times(1)).markAllUsedByUser(testUser);
        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
    }

    @Test
    void devrait_ne_rien_faire_si_l_utilisateur_n_existe_pas_lors_de_la_demande() {
        // Given
        when(userRepository.findByEmail("unknown@geolink.com")).thenReturn(Optional.empty());

        // When
        passwordResetService.requestReset("unknown@geolink.com");

        // Then
        verify(userRepository, times(1)).findByEmail("unknown@geolink.com");
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void devrait_reinitialiser_le_mot_de_passe_avec_succes() {
        // Given
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .user(testUser)
                .tokenHash("hashed_token")
                .expiryDate(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .build();

        when(jwtService.hashToken("raw_token")).thenReturn("hashed_token");
        when(passwordResetTokenRepository.findByTokenHash("hashed_token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPass123")).thenReturn("new_hash");

        // When
        passwordResetService.resetPassword("raw_token", "NewPass123");

        // Then
        assertThat(testUser.getPasswordHash()).isEqualTo("new_hash");
        assertThat(token.isUsed()).isTrue();
        verify(userRepository, times(1)).save(testUser);
        verify(passwordResetTokenRepository, times(1)).save(token);
        verify(refreshTokenService, times(1)).revokeAllForUser(testUser);
    }

    @Test
    void devrait_lever_une_exception_si_le_token_de_reinitialisation_est_expire() {
        // Given
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .user(testUser)
                .tokenHash("hashed_token")
                .expiryDate(Instant.now().minus(10, ChronoUnit.MINUTES))
                .used(false)
                .build();

        when(jwtService.hashToken("raw_token")).thenReturn("hashed_token");
        when(passwordResetTokenRepository.findByTokenHash("hashed_token")).thenReturn(Optional.of(token));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.resetPassword("raw_token", "NewPass123"))
                .isInstanceOf(InvalidOrExpiredTokenException.class);

        verify(userRepository, never()).save(any());
    }
}
