package com.geolink.findme.service.otpService;

import com.geolink.findme.entity.OtpCode;
import com.geolink.findme.entity.OtpPurpose;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.InvalidOrExpiredTokenException;
import com.geolink.findme.repository.OtpCodeRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final int MAX_ATTEMPTS = 5;

    private final OtpCodeRepository otpCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.ttl-minutes:10}")
    private int ttlMinutes;

    @Override
    @Transactional
    public String generate(User user, OtpPurpose purpose) {
        otpCodeRepository.consumeAllActiveForUserAndPurpose(user.getId(), purpose);

        String rawCode = String.format("%06d", secureRandom.nextInt(1_000_000));

        OtpCode otp = new OtpCode();
        otp.setUser(user);
        otp.setCodeHash(passwordEncoder.encode(rawCode));
        otp.setPurpose(purpose);
        otp.setExpiresAt(Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES));

        otpCodeRepository.save(otp);
        return rawCode;
    }

    @Override
    @Transactional
    public void verify(User user, OtpPurpose purpose, String rawCode) {
        OtpCode otp = otpCodeRepository.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(user.getId(), purpose)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Aucun code OTP actif, veuillez en redemander un"));

        if (otp.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidOrExpiredTokenException("Code OTP expiré", true);
        }

        if (otp.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new InvalidOrExpiredTokenException("Nombre maximal de tentatives atteint, veuillez redemander un code");
        }

        if (!passwordEncoder.matches(rawCode, otp.getCodeHash())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpCodeRepository.save(otp);
            throw new InvalidOrExpiredTokenException("Code OTP incorrect");
        }

        otp.setConsumed(true);
        otpCodeRepository.save(otp);
    }
}
