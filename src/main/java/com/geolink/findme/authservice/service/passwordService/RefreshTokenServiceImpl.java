package com.geolink.findme.authservice.service.passwordService;

import com.geolink.findme.authservice.entity.RefreshToken;
import com.geolink.findme.authservice.entity.User;
import com.geolink.findme.authservice.exception.InvalidOrExpiredTokenException;
import com.geolink.findme.authservice.repository.RefreshTokenRepository;
import com.geolink.findme.authservice.security.JwtService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Implémentation du service de gestion des refresh tokens avec rotation.
 */
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = jwtService.generateOpaqueToken();
        String hash = jwtService.hashToken(rawToken);
        Instant expiration = Instant.now().plus(jwtService.getRefreshExpirationInDays(), ChronoUnit.DAYS);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .expiration(expiration)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Override
    @Transactional
    public RefreshToken verifyAndRotate(String rawRefreshToken) {
        String hash = jwtService.hashToken(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Token de rafraîchissement invalide ou inexistant"));

        if (!token.isValid()) {
            throw new InvalidOrExpiredTokenException("Token de rafraîchissement invalide ou expiré");
        }

        // Révoque le token courant (rotation)
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        return token;
    }

    @Override
    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    @Override
    @Transactional
    public void revokeToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String hash = jwtService.hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }
}
