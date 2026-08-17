package com.geolink.findme.authservice.service.auth;

import com.geolink.findme.authservice.dto.mapper.UserMapper;
import com.geolink.findme.authservice.dto.request.RefreshRequestDTO;
import com.geolink.findme.authservice.dto.request.SignInRequestDTO;
import com.geolink.findme.authservice.dto.request.SignUpRequestDTO;
import com.geolink.findme.authservice.dto.response.AuthResponseDTO;
import com.geolink.findme.authservice.dto.response.UserProfileDTO;
import com.geolink.findme.authservice.entity.AccountStatus;
import com.geolink.findme.authservice.entity.RefreshToken;
import com.geolink.findme.authservice.entity.Role;
import com.geolink.findme.authservice.entity.User;
import com.geolink.findme.authservice.exception.EmailAlreadyUsedException;
import com.geolink.findme.authservice.exception.InvalidCredentialsException;
import com.geolink.findme.authservice.repository.RoleRepository;
import com.geolink.findme.authservice.repository.UserRepository;
import com.geolink.findme.authservice.security.JwtService;
import com.geolink.findme.authservice.security.UserPrincipal;
import com.geolink.findme.authservice.service.passwordService.RefreshTokenService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

/**
 * Implémentation du service d'authentification principal.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenService refreshTokenService,
            JwtService jwtService,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

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
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .roles(Set.of(userRole))
                .build();

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
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
}
