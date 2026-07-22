package com.example.findme.security;

import com.example.findme.entity.User;
import com.example.findme.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Filtre intercepteur qui s'execute une seule fois par requete HTTP.
 *
 * <p>Il est responsable de lire l'en-tete Authorization, d'extraire le token JWT,
 * de le faire valider par {@link JwtService}, et de charger l'utilisateur dans
 * le contexte de securite de Spring s'il est valide.</p>
 *
 * @author findme-team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Verifier si l'en-tete Authorization contient un Bearer Token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            // 2. Extraire l'email du token
            userEmail = jwtService.extractUsername(jwt);

            // 3. Si un email est trouve et qu'aucun utilisateur n'est encore authentifie
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                Optional<User> userOptional = userRepository.findByEmail(userEmail);

                if (userOptional.isPresent()) {
                    User user = userOptional.get();

                    // 4. Valider le token
                    if (jwtService.isTokenValid(jwt, user)) {

                        // 5. Creer le token d'authentification pour Spring Security
                        // L'autorite est basee sur le role (ex: ROLE_UTILISATEUR)
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                        );

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // 6. Placer l'utilisateur dans le SecurityContextHolder
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de la validation du JWT : {}", e.getMessage());
            // Si une exception se produit (ex: signature invalide, expire),
            // on ne bloque pas la requete ici, elle sera bloquee plus loin par Spring Security (qui verra qu'il n'y a pas d'authentification).
        }

        filterChain.doFilter(request, response);
    }
}
