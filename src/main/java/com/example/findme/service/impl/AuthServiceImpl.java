package com.example.findme.service.impl;

import com.example.findme.dto.request.SigninRequest;
import com.example.findme.dto.request.SignupRequest;
import com.example.findme.dto.response.AuthResponse;
import com.example.findme.dto.response.UserResponse;
import com.example.findme.entity.User;
import com.example.findme.enums.Role;
import com.example.findme.exception.DuplicateResourceException;
import com.example.findme.exception.ResourceNotFoundException;
import com.example.findme.repository.UserRepository;
import com.example.findme.security.JwtService;
import com.example.findme.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation de la logique metier pour l'authentification.
 *
 * @author findme-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        log.info("Tentative d'inscription pour l'email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Echec inscription : L'email {} est deja utilise", request.getEmail());
            throw new DuplicateResourceException("L'adresse email est deja utilisee");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Echec inscription : Le nom d'utilisateur {} est deja pris", request.getUsername());
            throw new DuplicateResourceException("Le nom d'utilisateur est deja pris");
        }

        System.out.println("Dans service implement");

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.UTILISATEUR)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Inscription reussie pour l'utilisateur ID: {}", savedUser.getId());

        // Token temporaire (FEATURE-7 va implementer le vrai JWT)
        String jwtToken = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .token(jwtToken)
                .user(mapToUserResponse(savedUser))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse signin(SigninRequest request) {
        log.info("Tentative de connexion pour l'email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Identifiants incorrects"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Echec connexion : Mot de passe invalide pour {}", request.getEmail());
            // On renvoie un Not Found generique plutot que "Mot de passe invalide" pour des raisons de securite
            throw new ResourceNotFoundException("Identifiants incorrects");
        }

        log.info("Connexion reussie pour l'utilisateur ID: {}", user.getId());

        // Token temporaire (FEATURE-7 va implementer le vrai JWT)
        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .user(mapToUserResponse(user))
                .build();
    }

    /**
     * Mapper manuel pour eviter d'exposer l'entite et le mot de passe.
     * (Un outil comme MapStruct pourrait etre utilise dans un projet plus large).
     */
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .photo(user.getPhoto())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
}
