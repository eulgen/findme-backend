package com.example.findme.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import lombok.RequiredArgsConstructor;

/**
 * Configuration principale de Spring Security.
 *
 * <p>Cette classe :</p>
 * <ul>
 *   <li>Desactive les protections CSRF (inutiles pour une API REST avec JWT).</li>
 *   <li>Configure la session en STATELESS (aucune session serveur n'est creee).</li>
 *   <li>Definit un {@link PasswordEncoder} utilisant l'algorithme BCrypt.</li>
 *   <li>Laisse l'API ouverte pour l'instant (les routes protegees seront
 *       configurees lors de l'implementation du JWT).</li>
 * </ul>
 *
 * @author findme-team
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    /**
     * Fournit un bean PasswordEncoder utilisant BCrypt pour hacher les mots de passe.
     * C'est l'algorithme recommande par defaut par Spring Security.
     *
     * @return une instance de BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configure la chaine de filtres de securite HTTP.
     *
     * @param http l'objet HttpSecurity a configurer
     * @return la SecurityFilterChain configuree
     * @throws Exception en cas d'erreur de configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        http
            // Desactivation du CSRF (inutile avec des tokens JWT et evite les erreurs 403)
            .csrf(AbstractHttpConfigurer::disable)
            
            // L'application est Stateless : pas de stockage d'etat de session
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Autorisation des requetes (pour le moment tout est autorise, 
            // la securisation fine sera ajoutee avec le filtre JWT)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll() // Routes publiques (inscription, login)
                .requestMatchers("/test/exceptions/**").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**", "/api-docs").permitAll() // Routes de test (a retirer en prod)
                .anyRequest().authenticated() // Toutes les autres requetes necessitent un JWT
            )
            // Ajouter le filtre JWT avant le filtre standard d'authentification par mot de passe
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
