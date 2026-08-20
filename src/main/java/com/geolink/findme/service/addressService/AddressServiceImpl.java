package com.geolink.findme.service.addressService;

import com.geolink.findme.dto.mapper.AddressMapper;
import com.geolink.findme.dto.request.AddressRequestDTO;
import com.geolink.findme.dto.response.AddressExportDTO;
import com.geolink.findme.dto.response.AddressResponseDTO;
import com.geolink.findme.entity.Address;
import com.geolink.findme.entity.GpsCoordinate;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.AddressNotFoundException;
import com.geolink.findme.exception.ForbiddenAccessException;
import com.geolink.findme.exception.MaxAddressLimitExceededException;
import com.geolink.findme.repository.AddressRepository;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.service.storageService.StorageService;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implémentation du service métier de gestion des adresses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressServiceImpl implements AddressService {

    private static final int MAX_ADDRESSES_PER_USER = 4;
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RANDOM = new SecureRandom();

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;
    private final StorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public Page<AddressResponseDTO> getUserAddresses(
            User user,
            String country,
            String city,
            String street,
            String search,
            Pageable pageable
    ) {
        Specification<Address> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Seules les adresses de l'utilisateur connecté
            Join<Address, User> userJoin = root.join("users");
            predicates.add(cb.equal(userJoin.get("id"), user.getId()));

            if (country != null && !country.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("country")), "%" + country.toLowerCase().trim() + "%"));
            }

            if (city != null && !city.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase().trim() + "%"));
            }

            if (street != null && !street.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("street")), "%" + street.toLowerCase().trim() + "%"));
            }

            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.toLowerCase().trim() + "%";
                Predicate searchCode = cb.like(cb.lower(root.get("addressCode")), searchPattern);
                Predicate searchCountry = cb.like(cb.lower(root.get("country")), searchPattern);
                Predicate searchCity = cb.like(cb.lower(root.get("city")), searchPattern);
                Predicate searchStreet = cb.like(cb.lower(root.get("street")), searchPattern);
                Predicate searchHouseNumber = cb.like(cb.lower(root.get("houseNumber")), searchPattern);
                predicates.add(cb.or(searchCode, searchCountry, searchCity, searchStreet, searchHouseNumber));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Address> page = addressRepository.findAll(spec, pageable);
        return page.map(address -> addressMapper.toDTO(address, storageService));
    }

    @Override
    @Transactional
    public AddressResponseDTO createAddress(User user, AddressRequestDTO requestDTO) {
        long currentCount = addressRepository.countByUsers_Id(user.getId());
        if (currentCount >= MAX_ADDRESSES_PER_USER) {
            log.warn("Tentative de création d'une 5ème adresse refusée pour l'utilisateur ID: {}", user.getId());
            throw new MaxAddressLimitExceededException(
                    "Vous possédez déjà " + currentCount + " adresses. La limite maximale est fixée à " + MAX_ADDRESSES_PER_USER + " adresses par utilisateur."
            );
        }

        Address address = addressMapper.toEntity(requestDTO);
        address.setAddressCode(generateUniqueAddressCode());

        // Association réciproque Many-to-Many
        address.getUsers().add(user);
        user.getAddresses().add(address);

        Address savedAddress = addressRepository.save(address);
        userRepository.save(user);

        log.info("Adresse créée avec succès ID: {}, Code: {} pour l'utilisateur ID: {}",
                savedAddress.getId(), savedAddress.getAddressCode(), user.getId());

        return addressMapper.toDTO(savedAddress, storageService);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponseDTO getAddressById(User user, Long addressId) {
        Address address = findAddressAndCheckOwnership(user, addressId);
        return addressMapper.toDTO(address, storageService);
    }

    @Override
    @Transactional
    public AddressResponseDTO updateAddress(User user, Long addressId, AddressRequestDTO requestDTO) {
        Address address = findAddressAndCheckOwnership(user, addressId);

        if (requestDTO.getCountry() != null && !requestDTO.getCountry().isBlank()) {
            address.setCountry(requestDTO.getCountry().trim());
        }
        if (requestDTO.getCity() != null && !requestDTO.getCity().isBlank()) {
            address.setCity(requestDTO.getCity().trim());
        }
        if (requestDTO.getStreet() != null) {
            address.setStreet(requestDTO.getStreet().trim());
        }
        if (requestDTO.getHouseNumber() != null) {
            address.setHouseNumber(requestDTO.getHouseNumber().trim());
        }

        if (requestDTO.getGps() != null) {
            if (address.getGpsCoordinate() == null) {
                address.setGpsCoordinate(new GpsCoordinate());
            }
            if (requestDTO.getGps().getLatitude() != null) {
                address.getGpsCoordinate().setLatitude(requestDTO.getGps().getLatitude());
            }
            if (requestDTO.getGps().getLongitude() != null) {
                address.getGpsCoordinate().setLongitude(requestDTO.getGps().getLongitude());
            }
        }

        Address updatedAddress = addressRepository.save(address);
        log.info("Adresse modifiée avec succès ID: {} par l'utilisateur ID: {}", addressId, user.getId());

        return addressMapper.toDTO(updatedAddress, storageService);
    }

    @Override
    @Transactional
    public void deleteAddress(User user, Long addressId) {
        Address address = findAddressAndCheckOwnership(user, addressId);

        address.getUsers().remove(user);
        user.getAddresses().remove(address);

        if (address.getUsers().isEmpty()) {
            if (address.getPhotoUrl() != null) {
                storageService.delete(address.getPhotoUrl(), "addresses");
            }
            addressRepository.delete(address);
            log.info("Adresse ID: {} supprimée définitivement car orpheline", addressId);
        } else {
            addressRepository.save(address);
            log.info("Lien vers l'adresse ID: {} retiré pour l'utilisateur ID: {}", addressId, user.getId());
        }
    }

    @Override
    @Transactional
    public AddressResponseDTO uploadPhoto(User user, Long addressId, MultipartFile file) {
        Address address = findAddressAndCheckOwnership(user, addressId);

        if (address.getPhotoUrl() != null && !address.getPhotoUrl().isBlank()) {
            storageService.delete(address.getPhotoUrl(), "addresses");
        }

        String filename = storageService.store(file, "addresses");
        address.setPhotoUrl(filename);

        Address updatedAddress = addressRepository.save(address);
        log.info("Photo mise à jour pour l'adresse ID: {} -> {}", addressId, filename);

        return addressMapper.toDTO(updatedAddress, storageService);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressExportDTO exportAddressPdfData(User user, Long addressId) {
        Address address = findAddressAndCheckOwnership(user, addressId);
        return addressMapper.toExportDTO(address, user, storageService);
    }

    private Address findAddressAndCheckOwnership(User user, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new AddressNotFoundException("Adresse non trouvée avec l'identifiant : " + addressId));

        boolean isOwner = address.getUsers().stream().anyMatch(u -> u.getId().equals(user.getId()));
        if (!isOwner) {
            log.warn("Accès refusé : L'utilisateur ID {} tente d'accéder à l'adresse ID {}", user.getId(), addressId);
            throw new ForbiddenAccessException("Accès refusé : Cette adresse ne vous appartient pas.");
        }

        return address;
    }

    private String generateUniqueAddressCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("ADR-2026-");
            for (int i = 0; i < 4; i++) {
                sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
            }
            code = sb.toString();
        } while (addressRepository.existsByAddressCode(code));

        return code;
    }
}
