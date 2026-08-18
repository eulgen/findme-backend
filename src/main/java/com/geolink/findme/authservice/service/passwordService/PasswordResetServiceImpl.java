package com.geolink.findme.authservice.service.passwordService;

import com.geolink.findme.authservice.entity.OtpPurpose;
import com.geolink.findme.authservice.entity.User;
import com.geolink.findme.authservice.exception.InvalidOrExpiredTokenException;
import com.geolink.findme.authservice.repository.UserRepository;
import com.geolink.findme.authservice.service.EmailService;
import com.geolink.findme.authservice.service.OtpService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;

    @Value("${app.otp.ttl-minutes:10}")
    private int otpTtlMinutes;

    @Override
    @Transactional
    public void requestReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("Demande de réinitialisation reçue pour un email inexistant : {}", email);
            return; // Anti-account enumeration : réponse 200 systématique
        }

        User user = userOpt.get();
        String otp = otpService.generate(user, OtpPurpose.PASSWORD_RESET);
        emailService.sendOtpEmail(
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                otp,
                otpTtlMinutes,
                OtpPurpose.PASSWORD_RESET
        );
        log.info("Code OTP de réinitialisation généré et envoyé pour l'utilisateur id={}", user.getId());
    }

    @Override
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Code OTP de réinitialisation invalide ou inexistant"));

        otpService.verify(user, OtpPurpose.PASSWORD_RESET, code);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(user);
    }
}
