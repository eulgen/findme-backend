package com.geolink.findme.authservice.service.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geolink.findme.authservice.dto.request.AppleLoginRequestDTO;
import com.geolink.findme.authservice.dto.request.GoogleLoginRequestDTO;
import com.geolink.findme.authservice.dto.response.AuthResponseDTO;
import com.geolink.findme.authservice.entity.AccountStatus;
import com.geolink.findme.authservice.entity.AuthProvider;
import com.geolink.findme.authservice.entity.Role;
import com.geolink.findme.authservice.entity.User;
import com.geolink.findme.authservice.exception.InvalidCredentialsException;
import com.geolink.findme.authservice.repository.RoleRepository;
import com.geolink.findme.authservice.repository.UserRepository;
import com.geolink.findme.authservice.security.JwtService;
import com.geolink.findme.authservice.security.UserPrincipal;
import com.geolink.findme.authservice.service.passwordService.RefreshTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

/**
 * Implémentation du service d'authentification sociale (Google & Apple).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialAuthServiceImpl implements SocialAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public AuthResponseDTO authenticateWithGoogle(GoogleLoginRequestDTO dto) {
        String email = extractEmailFromIdToken(dto.getIdToken());
        String name = extractClaimFromIdToken(dto.getIdToken(), "name");

        if (email == null || email.isBlank()) {
            throw new InvalidCredentialsException("Jeton Google invalide : email introuvable");
        }

        User user = userRepository.findByEmail(email)
                .map(existing -> linkProviderIfNeeded(existing, AuthProvider.GOOGLE))
                .orElseGet(() -> createSocialUser(email, name, null, null, AuthProvider.GOOGLE));

        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponseDTO authenticateWithApple(AppleLoginRequestDTO dto) {
        String email = extractEmailFromIdToken(dto.getIdToken());

        if (email == null || email.isBlank()) {
            throw new InvalidCredentialsException("Jeton Apple invalide : email introuvable");
        }

        User user = userRepository.findByEmail(email)
                .map(existing -> linkProviderIfNeeded(existing, AuthProvider.APPLE))
                .orElseGet(() -> createSocialUser(email, null, dto.getFirstName(), dto.getLastName(), AuthProvider.APPLE));

        return generateAuthResponse(user);
    }

    private User createSocialUser(String email, String fullName, String firstName, String lastName, AuthProvider provider) {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Rôle USER introuvable"));

        String finalFirstName = firstName;
        String finalLastName = lastName;

        if ((finalFirstName == null || finalFirstName.isBlank()) && fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.split(" ", 2);
            finalFirstName = parts[0];
            finalLastName = parts.length > 1 ? parts[1] : "";
        }

        if (finalFirstName == null || finalFirstName.isBlank()) {
            finalFirstName = email;
            finalLastName = "";
        }

        User user = User.builder()
                .email(email)
                .firstName(finalFirstName)
                .lastName(finalLastName != null ? finalLastName : "")
                .passwordHash(null)
                .provider(provider)
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .createdAt(Instant.now())
                .roles(Set.of(userRole))
                .build();

        return userRepository.save(user);
    }

    private User linkProviderIfNeeded(User existing, AuthProvider provider) {
        if (!existing.isAccountVerified()) {
            existing.setAccountVerified(true);
        }
        return existing;
    }

    private AuthResponseDTO generateAuthResponse(User user) {
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

    private String extractEmailFromIdToken(String idToken) {
        return extractClaimFromIdToken(idToken, "email");
    }

    private String extractClaimFromIdToken(String idToken, String claimName) {
        try {
            if (idToken == null || !idToken.contains(".")) {
                return null;
            }
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(payloadJson);
            return node.has(claimName) ? node.get(claimName).asText() : null;
        } catch (Exception e) {
            log.warn("Impossible de lire la claim '{}' du token OAuth2 : {}", claimName, e.getMessage());
            return null;
        }
    }
}
