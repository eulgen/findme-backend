package com.example.findme.enums;

/**
 * Enumeration des roles disponibles dans l'application FindMe.
 *
 * <p>Chaque utilisateur possede exactement un role qui determine
 * ses droits d'acces aux ressources de l'API :</p>
 * <ul>
 *   <li>{@link #UTILISATEUR} : acces aux fonctionnalites standard
 *       (gestion de ses propres adresses, envoi de messages de support).</li>
 *   <li>{@link #ADMIN} : acces complet incluant la gestion de tous les
 *       utilisateurs, adresses et messages de support.</li>
 * </ul>
 *
 * @author findme-team
 * @version 1.0.0
 */
public enum Role {

    /**
     * Role standard attribue par defaut lors de l'inscription.
     * Permet de gerer ses propres adresses et d'envoyer des messages de support.
     */
    UTILISATEUR,

    /**
     * Role administrateur avec acces complet a toutes les ressources.
     * Permet la gestion des utilisateurs, adresses et messages de support.
     */
    ADMIN
}
