package com.geolink.findme.service.addressService;

import com.geolink.findme.dto.request.AddressRequestDTO;
import com.geolink.findme.dto.response.AddressExportDTO;
import com.geolink.findme.dto.response.AddressResponseDTO;
import com.geolink.findme.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface du service de gestion métier des adresses et de l'application de la règle des 4 adresses.
 */
public interface AddressService {

    /**
     * Retourne la liste paginée, triée et filtrée des adresses de l'utilisateur connecté.
     */
    Page<AddressResponseDTO> getUserAddresses(
            User user,
            String country,
            String city,
            String street,
            String search,
            Pageable pageable
    );

    /**
     * Crée une nouvelle adresse et la rattache à l'utilisateur connecté (max 4 adresses).
     */
    AddressResponseDTO createAddress(User user, AddressRequestDTO requestDTO);

    /**
     * Récupère le détail d'une adresse de l'utilisateur connecté.
     */
    AddressResponseDTO getAddressById(User user, Long addressId);

    /**
     * Modifie une adresse existante appartenant à l'utilisateur connecté.
     */
    AddressResponseDTO updateAddress(User user, Long addressId, AddressRequestDTO requestDTO);

    /**
     * Supprime le lien d'adresse pour l'utilisateur connecté (et supprime l'adresse si orpheline).
     */
    void deleteAddress(User user, Long addressId);

    /**
     * Upload ou remplace la photo associée à une adresse de l'utilisateur connecté.
     */
    AddressResponseDTO uploadPhoto(User user, Long addressId, MultipartFile file);

    /**
     * Génère et retourne le DTO d'export PDF complet pour le rendu frontend.
     */
    AddressExportDTO exportAddressPdfData(User user, Long addressId);
}
