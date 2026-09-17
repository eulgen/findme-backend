package com.geolink.findme.unit.conventionnel;

import com.geolink.findme.entity.RefreshToken;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.InvalidOrExpiredTokenException;
import com.geolink.findme.repository.RefreshTokenRepository;
import com.geolink.findme.security.JwtService;
import com.geolink.findme.service.passwordService.RefreshTokenServiceImpl;

import org.junit.jupiter.api.BeforeEach;
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

class RefreshTokenServiceImplConventionnelTest {


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
    void devrait_creer_un_refresh_token_avec_succes() {
        // Given
        when(jwtService.generateOpaqueToken()).thenReturn("raw_token");
        when(jwtService.hashToken("raw_token")).thenReturn("hashed_token");
        when(jwtService.getRefreshExpirationInDays()).thenReturn(7L);

        // When
        String result = refreshTokenService.createRefreshToken(testUser);

        // Then
        assertThat(result).isEqualTo("raw_token");
        verify(jwtService, times(1)).generateOpaqueToken();
        verify(jwtService, times(1)).hashToken("raw_token");
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void devrait_verifier_et_rotater_le_token_avec_succes() {
        // Given
        RefreshToken token = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .tokenHash("hashed_token")
                .expiration(Instant.now().plus(1, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        when(jwtService.hashToken("raw_token")).thenReturn("hashed_token");
        when(refreshTokenRepository.findByTokenHash("hashed_token")).thenReturn(Optional.of(token));

        // When
        RefreshToken rotated = refreshTokenService.verifyAndRotate("raw_token");

        // Then
        assertThat(rotated).isNotNull();
        assertThat(token.isRevoked()).isTrue();
        verify(jwtService, times(1)).hashToken("raw_token");
        verify(refreshTokenRepository, times(1)).findByTokenHash("hashed_token");
        verify(refreshTokenRepository, times(1)).save(token);
    }

    @Test
    void devrait_revoquer_tous_les_tokens_de_l_utilisateur() {
        // Given & When
        refreshTokenService.revokeAllForUser(testUser);

        // Then
        verify(refreshTokenRepository, times(1)).revokeAllByUser(testUser);
    }
}
