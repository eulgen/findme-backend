package com.geolink.findme.service.auth;

import com.geolink.findme.dto.mapper.UserMapper;
import com.geolink.findme.dto.request.RefreshRequestDTO;
import com.geolink.findme.dto.request.SignInRequestDTO;
import com.geolink.findme.dto.request.SignUpRequestDTO;
import com.geolink.findme.dto.response.AuthResponseDTO;
import com.geolink.findme.dto.response.UserProfileDTO;
import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.OtpPurpose;
import com.geolink.findme.entity.RefreshToken;
import com.geolink.findme.entity.Role;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.EmailAlreadyUsedException;
import com.geolink.findme.exception.InvalidCredentialsException;
import com.geolink.findme.exception.InvalidOrExpiredTokenException;
import com.geolink.findme.repository.RoleRepository;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.security.JwtService;
import com.geolink.findme.security.UserPrincipal;
import com.geolink.findme.service.emailService.EmailService;
import com.geolink.findme.service.otpService.OtpService;
import com.geolink.findme.service.passwordService.RefreshTokenService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

/**
 * Implémentation du service d'authentification principal.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;

    @Value("${app.otp.ttl-minutes:10}")
    private int otpTtlMinutes;

    @Override
    @Transactional(rollbackFor = EmailAlreadyUsedException.class)
    public UserProfileDTO signUp(SignUpRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyUsedException("Un compte existe déjà avec l'adresse " + dto.getEmail());
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Le rôle USER de référence est introuvable en base"));

        User user = User.builder()
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .roles(Set.of(userRole))
                .build();

        user.setAccountVerified(false);
        User savedUser = userRepository.save(user);

        String otp = otpService.generate(savedUser, OtpPurpose.ACCOUNT_VERIFICATION);
        emailService.sendOtpEmail(
                savedUser.getEmail(),
                savedUser.getFullName(),
                otp,
                otpTtlMinutes,
                OtpPurpose.ACCOUNT_VERIFICATION
        );

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public void verifyAccount(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Utilisateur introuvable"));

        otpService.verify(user, OtpPurpose.ACCOUNT_VERIFICATION, code);
        user.setAccountVerified(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resendVerificationOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Utilisateur introuvable"));

        if (user.isAccountVerified()) {
            throw new InvalidOrExpiredTokenException("Le compte est déjà vérifié");
        }

        String otp = otpService.generate(user, OtpPurpose.ACCOUNT_VERIFICATION);
        emailService.sendOtpEmail(
                user.getEmail(),
                user.getFullName(),
                otp,
                otpTtlMinutes,
                OtpPurpose.ACCOUNT_VERIFICATION
        );
    }

    @Override
    @Transactional
    public AuthResponseDTO signIn(SignInRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Email ou mot de passe incorrect");
        }

        if (!user.isActive()) {
            if (!user.isAccountVerified()) {
                throw new InvalidCredentialsException("Veuillez vérifier votre compte par mail avant de vous connecter");
            }
            throw new InvalidCredentialsException("Compte désactivé ou inactif");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessExpirationInSeconds())
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO refresh(RefreshRequestDTO dto) {
        RefreshToken token = refreshTokenService.verifyAndRotate(dto.getRefreshToken());
        User user = token.getUser();

        String newRefreshToken = refreshTokenService.createRefreshToken(user);
        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtService.getAccessExpirationInSeconds())
                .build();
    }

    @Override
    @Transactional
    public void logout(String email) {
        userRepository.findByEmail(email).ifPresent(refreshTokenService::revokeAllForUser);
    }

    @Override
    @Transactional
    public void logoutWithToken(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }
}
