package com.geolink.findme.dto.mapper;

import com.geolink.findme.dto.request.AddressRequestDTO;
import com.geolink.findme.dto.response.AddressExportDTO;
import com.geolink.findme.dto.response.AddressResponseDTO;
import com.geolink.findme.dto.response.GpsCoordinateDTO;
import com.geolink.findme.dto.response.UserPdfExportDTO;
import com.geolink.findme.entity.Address;
import com.geolink.findme.entity.GpsCoordinate;
import com.geolink.findme.entity.User;
import com.geolink.findme.service.storageService.StorageService;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper composant Spring gérant les conversions entre les entités JPA (Address, GpsCoordinate) et leurs DTOs.
 */
@Component
public class AddressMapper {

    public GpsCoordinateDTO toDTO(GpsCoordinate entity) {
        if (entity == null) {
            return null;
        }
        return GpsCoordinateDTO.builder()
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .build();
    }

    public GpsCoordinate toEntity(GpsCoordinateDTO dto) {
        if (dto == null) {
            return null;
        }
        return GpsCoordinate.builder()
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();
    }

    public Address toEntity(AddressRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        Address address = Address.builder()
                .country(dto.getCountry())
                .city(dto.getCity())
                .street(dto.getStreet())
                .houseNumber(dto.getHouseNumber())
                .gpsCoordinate(toEntity(dto.getGps()))
                .build();
        return address;
    }

    public AddressResponseDTO toDTO(Address address, StorageService storageService) {
        if (address == null) {
            return null;
        }

        String photoPublicUrl = null;
        if (address.getPhotoUrl() != null && !address.getPhotoUrl().isBlank()) {
            photoPublicUrl = storageService != null
                    ? storageService.getPublicUrl(address.getPhotoUrl(), "addresses")
                    : address.getPhotoUrl();
        }

        return AddressResponseDTO.builder()
                .id(address.getId())
                .addressCode(address.getAddressCode())
                .country(address.getCountry())
                .city(address.getCity())
                .street(address.getStreet())
                .houseNumber(address.getHouseNumber())
                .photoUrl(photoPublicUrl)
                .gps(toDTO(address.getGpsCoordinate()))
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    public AddressExportDTO toExportDTO(Address address, User user, StorageService storageService) {
        if (address == null) {
            return null;
        }

        String photoPublicUrl = null;
        if (address.getPhotoUrl() != null && !address.getPhotoUrl().isBlank()) {
            photoPublicUrl = storageService != null
                    ? storageService.getPublicUrl(address.getPhotoUrl(), "addresses")
                    : address.getPhotoUrl();
        }

        UserPdfExportDTO userDto = null;
        if (user != null) {
            userDto = UserPdfExportDTO.builder()
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phoneNumber(user.getPhoneNumber())
                    .build();
        }

        return AddressExportDTO.builder()
                .addressId(address.getId())
                .addressCode(address.getAddressCode())
                .user(userDto)
                .formattedAddress(formatAddress(address))
                .country(address.getCountry())
                .city(address.getCity())
                .street(address.getStreet())
                .houseNumber(address.getHouseNumber())
                .gps(toDTO(address.getGpsCoordinate()))
                .photoUrl(photoPublicUrl)
                .generatedAt(Instant.now())
                .build();
    }

    public String formatAddress(Address address) {
        if (address == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (address.getHouseNumber() != null && !address.getHouseNumber().isBlank()) {
            parts.add(address.getHouseNumber());
        }
        if (address.getStreet() != null && !address.getStreet().isBlank()) {
            parts.add(address.getStreet());
        }
        if (address.getCity() != null && !address.getCity().isBlank()) {
            parts.add(address.getCity());
        }
        if (address.getCountry() != null && !address.getCountry().isBlank()) {
            parts.add(address.getCountry());
        }
        return String.join(", ", parts);
    }
}
