package com.geolink.findme.entity;

/**
 * Énumération représentant les statuts possibles d'une adresse dans la plateforme FindMe.
 */
public enum AddressStatus {
    /**
     * L'adresse est en attente de validation par l'administrateur.
     */
    EN_ATTENTE,

    /**
     * L'adresse a été validée par l'administrateur.
     */
    VALIDE,

    /**
     * L'adresse a été marquée comme non valide par l'administrateur.
     */
    NON_VALIDE
}
