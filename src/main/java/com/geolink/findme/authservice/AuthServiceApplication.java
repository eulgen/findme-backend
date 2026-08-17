package com.geolink.findme.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'application auth-service.
 * <p>
 * Microservice responsable de l'authentification, de la gestion des comptes
 * utilisateurs et de l'émission/validation des tokens JWT pour la plateforme findMe.
 * </p>
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
