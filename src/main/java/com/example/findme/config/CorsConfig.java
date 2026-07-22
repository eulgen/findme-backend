package com.example.findme.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration globale des politiques CORS (Cross-Origin Resource Sharing).
 *
 * <p>Permet aux clients web (navigateurs) situes sur d'autres domaines (ex: frontend React, Vue)
 * de faire des requetes vers cette API REST sans etre bloques par les navigateurs.</p>
 *
 * @author findme-team
 * @version 1.0.0
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Applique a tous les endpoints de l'API
                        .allowedOriginPatterns("*") // Autorise toutes les origines (a restreindre en production)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // Methodes HTTP autorisees
                        .allowedHeaders("*") // Autorise tous les en-tetes HTTP (dont Authorization)
                        .allowCredentials(true) // Permet l'envoi de cookies/credentials si necessaire
                        .maxAge(3600); // Met en cache la reponse preflight OPTIONS pendant 1 heure
            }
        };
    }
}
