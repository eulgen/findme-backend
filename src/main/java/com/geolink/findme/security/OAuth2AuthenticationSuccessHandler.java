package com.geolink.findme.security;

import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.AuthProvider;
import com.geolink.findme.entity.Role;
import com.geolink.findme.entity.User;
import com.geolink.findme.repository.RoleRepository;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.service.passwordService.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

/**
 * Handler de succès d'authentification OAuth2 (Google & Apple).
 * Transforme l'identité vérifiée par le provider en jetons JWT local + Refresh Token
 * et redirige vers le frontend.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (email == null || email.isBlank()) {
            log.error("Email introuvable dans le profil OAuth2 du provider {}", registrationId);
            response.sendRedirect(frontendBaseUrl + "/login?error=email_missing");
            return;
        }

        User user = userRepository.findByEmail(email)
                .map(existing -> linkProviderIfNeeded(existing, registrationId))
                .orElseGet(() -> createFromProvider(email, name, registrationId));

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        String targetUrl = UriComponentsBuilder
                .fromUriString(frontendBaseUrl + "/oauth2/callback")
                .queryParam("access_token", accessToken)
                .queryParam("refresh_token", refreshToken)
                .build().toUriString();

        response.sendRedirect(targetUrl);
    }

    private User createFromProvider(String email, String name, String registrationId) {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Rôle USER introuvable"));

        String fullName = (name != null && !name.isBlank()) ? name : email;

        AuthProvider provider;
        try {
            provider = AuthProvider.valueOf(registrationId.toUpperCase());
        } catch (Exception e) {
            provider = AuthProvider.LOCAL;
        }

        User user = User.builder()
                .email(email)
                .fullName(fullName)
                .passwordHash(null)
                .provider(provider)
                .status(AccountStatus.ACTIVE)
                .accountVerified(true)
                .createdAt(Instant.now())
                .roles(Set.of(userRole))
                .build();

        return userRepository.save(user);
    }

    private User linkProviderIfNeeded(User existing, String registrationId) {
        if (!existing.isAccountVerified()) {
            existing.setAccountVerified(true);
        }
        return existing;
    }
}
