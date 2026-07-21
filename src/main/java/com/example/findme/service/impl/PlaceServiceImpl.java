package com.example.findme.service.impl;

import com.example.findme.dto.request.PlaceRequest;
import com.example.findme.dto.response.PlaceResponse;
import com.example.findme.entity.Place;
import com.example.findme.entity.User;
import com.example.findme.exception.LimitExceededException;
import com.example.findme.exception.ResourceNotFoundException;
import com.example.findme.repository.PlaceRepository;
import com.example.findme.repository.UserRepository;
import com.example.findme.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceServiceImpl implements PlaceService {

    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PlaceResponse createPlace(PlaceRequest request, String username) {
        User user = getUserByUsername(username);

        // Verification de la limite de 4 adresses
        List<Place> userPlaces = placeRepository.findByUserId(user.getId());
        if (userPlaces.size() >= 4) {
            throw new LimitExceededException("Vous avez atteint la limite maximale de 4 adresses.");
        }

        Place place = Place.builder()
                .name(request.getName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .type(request.getType())
                .user(user)
                .build();

        Place savedPlace = placeRepository.save(place);
        return mapToResponse(savedPlace);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlaceResponse> getUserPlaces(String username) {
        User user = getUserByUsername(username);
        return placeRepository.findByUserId(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceResponse getPlaceById(Long id, String username) {
        User user = getUserByUsername(username);
        Place place = getPlaceOwnedByUser(id, user.getId());
        return mapToResponse(place);
    }

    @Override
    @Transactional
    public PlaceResponse updatePlace(Long id, PlaceRequest request, String username) {
        User user = getUserByUsername(username);
        Place place = getPlaceOwnedByUser(id, user.getId());

        place.setName(request.getName());
        place.setAddress(request.getAddress());
        place.setLatitude(request.getLatitude());
        place.setLongitude(request.getLongitude());
        place.setType(request.getType());

        Place updatedPlace = placeRepository.save(place);
        return mapToResponse(updatedPlace);
    }

    @Override
    @Transactional
    public void deletePlace(Long id, String username) {
        User user = getUserByUsername(username);
        Place place = getPlaceOwnedByUser(id, user.getId());
        placeRepository.delete(place);
    }

    // --- Utilitaires ---

    private User getUserByUsername(String username) {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private Place getPlaceOwnedByUser(Long placeId, Long userId) {
        return placeRepository.findByIdAndUserId(placeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Lieu introuvable ou vous n'avez pas l'autorisation d'y acceder"));
    }

    private PlaceResponse mapToResponse(Place place) {
        return PlaceResponse.builder()
                .id(place.getId())
                .name(place.getName())
                .address(place.getAddress())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .type(place.getType())
                .createdAt(place.getCreatedAt())
                .updatedAt(place.getUpdatedAt())
                .build();
    }
}
