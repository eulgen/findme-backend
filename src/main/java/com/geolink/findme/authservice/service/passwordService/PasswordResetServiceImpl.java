package com.geolink.findme.authservice.service.passwordService;

import com.geolink.findme.authservice.entity.PasswordResetToken;
import com.geolink.findme.authservice.entity.User;
import com.geolink.findme.authservice.exception.InvalidOrExpiredTokenException;
import com.geolink.findme.authservice.repository.PasswordResetTokenRepository;
import com.geolink.findme.authservice.repository.UserRepository;
import com.geolink.findme.authservice.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Implémentation du service de réinitialisation de mot de passe.
 */
@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final System.Logger log = System.getLogger(PasswordResetServiceImpl.class.getName());

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetServiceImpl(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RefreshTokenService refreshTokenService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void requestReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.log(System.Logger.Level.INFO, "Demande de réinitialisation reçue pour un email inexistant : {0}", email);
            return; // Anti-account enumeration : réponse 200 systématique
        }

        User user = userOpt.get();
        passwordResetTokenRepository.markAllUsedByUser(user);

        String rawToken = jwtService.generateOpaqueToken();
        String hash = jwtService.hashToken(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(hash)
                .expiryDate(Instant.now().plus(30, ChronoUnit.MINUTES))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        log.log(System.Logger.Level.INFO, "Token de réinitialisation généré pour l'utilisateur id={0}", user.getId());
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String hash = jwtService.hashToken(token);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Token de réinitialisation invalide ou inexistant"));

        if (resetToken.isUsed()) {
            throw new InvalidOrExpiredTokenException("Token de réinitialisation déjà utilisé");
        }

        if (resetToken.isExpired()) {
            throw new InvalidOrExpiredTokenException("Token de réinitialisation expiré", true);
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.markUsed();
        passwordResetTokenRepository.save(resetToken);

        refreshTokenService.revokeAllForUser(user);
    }
}
