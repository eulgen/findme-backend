package com.geolink.findme.unit.non_conventionnel;

import com.geolink.findme.entity.RefreshToken;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.InvalidOrExpiredTokenException;
import com.geolink.findme.repository.RefreshTokenRepository;
import com.geolink.findme.security.JwtService;
import com.geolink.findme.service.passwordService.RefreshTokenServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

class RefreshTokenServiceImplNonConventionnelTest {


    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("test@geolink.com").build();
    }

    @Test
    void devrait_lever_une_exception_si_le_refresh_token_est_expire() {
        // Given
        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .tokenHash("hashed_token")
                .expiration(Instant.now().minus(1, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        when(jwtService.hashToken("raw_token")).thenReturn("hashed_token");
        when(refreshTokenRepository.findByTokenHash("hashed_token")).thenReturn(Optional.of(expiredToken));

        // When & Then
        assertThatThrownBy(() -> refreshTokenService.verifyAndRotate("raw_token"))
                .isInstanceOf(InvalidOrExpiredTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait lever InvalidOrExpiredTokenException et révoquer tous les jetons lors d'une réutilisation d'un token révoqué")
    void devrait_lever_une_exception_et_revoquer_tout_si_le_refresh_token_est_revoque() {
        // Given
        RefreshToken revokedToken = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .tokenHash("hashed_token")
                .expiration(Instant.now().plus(1, ChronoUnit.DAYS))
                .revoked(true)
                .build();

        when(jwtService.hashToken("raw_token")).thenReturn("hashed_token");
        when(refreshTokenRepository.findByTokenHash("hashed_token")).thenReturn(Optional.of(revokedToken));

        // When & Then
        assertThatThrownBy(() -> refreshTokenService.verifyAndRotate("raw_token"))
                .isInstanceOf(InvalidOrExpiredTokenException.class);

        verify(refreshTokenRepository, times(1)).revokeAllByUser(testUser);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait lever InvalidOrExpiredTokenException si le refresh token est introuvable en base")
    void devrait_lever_exception_si_refresh_token_inexistant_en_base() {
        when(jwtService.hashToken("unknown_token")).thenReturn("unknown_hash");
        when(refreshTokenRepository.findByTokenHash("unknown_hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verifyAndRotate("unknown_token"))
                .isInstanceOf(InvalidOrExpiredTokenException.class)
                .hasMessageContaining("invalide ou inexistant");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait ignorer silencieusement la révocation si le token fourni est null")
    void devrait_ignorer_silencieusement_si_token_null() {
        refreshTokenService.revokeToken(null);

        verify(refreshTokenRepository, never()).findByTokenHash(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait ignorer silencieusement la révocation si le token fourni est une chaîne vide")
    void devrait_ignorer_silencieusement_si_token_vide() {
        refreshTokenService.revokeToken("   ");

        verify(refreshTokenRepository, never()).findByTokenHash(any());
        verify(refreshTokenRepository, never()).save(any());
    }
}
