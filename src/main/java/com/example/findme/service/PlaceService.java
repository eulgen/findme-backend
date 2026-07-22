package com.example.findme.service;

import com.example.findme.dto.request.PlaceRequest;
import com.example.findme.dto.response.PlaceResponse;

import java.util.List;

/**
 * Interface decrivant les operations metiers liees aux lieux (Places).
 */
public interface PlaceService {
    PlaceResponse createPlace(PlaceRequest request, String username);
    List<PlaceResponse> getUserPlaces(String username);
    PlaceResponse getPlaceById(Long id, String username);
    PlaceResponse updatePlace(Long id, PlaceRequest request, String username);
    void deletePlace(Long id, String username);
}
