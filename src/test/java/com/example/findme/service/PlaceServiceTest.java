package com.example.findme.service;

import com.example.findme.dto.request.PlaceRequest;
import com.example.findme.dto.response.PlaceResponse;
import com.example.findme.entity.Place;
import com.example.findme.entity.User;
import com.example.findme.exception.LimitExceededException;
import com.example.findme.exception.ResourceNotFoundException;
import com.example.findme.repository.PlaceRepository;
import com.example.findme.repository.UserRepository;
import com.example.findme.service.impl.PlaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du PlaceService")
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PlaceServiceImpl placeService;

    private User mockUser;
    private Place mockPlace;
    private PlaceRequest placeRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).username("testuser").build();
        
        mockPlace = Place.builder()
                .id(100L)
                .name("Maison")
                .address("123 rue de Paris")
                .user(mockUser)
                .build();

        placeRequest = PlaceRequest.builder()
                .name("Nouveau Lieu")
                .address("456 avenue de Lyon")
                .build();
    }

    @Test
    @DisplayName("createPlace() - Succes")
    void createPlace_shouldReturnPlaceResponse_whenValid() {
        when(userRepository.findByEmail("testuser")).thenReturn(Optional.of(mockUser));
        when(placeRepository.findByUserId(1L)).thenReturn(new ArrayList<>()); // 0 lieux
        when(placeRepository.save(any(Place.class))).thenAnswer(i -> {
            Place p = i.getArgument(0);
            p.setId(200L);
            return p;
        });

        PlaceResponse response = placeService.createPlace(placeRequest, "testuser");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(200L);
        assertThat(response.name()).isEqualTo("Nouveau Lieu");
    }

    @Test
    @DisplayName("createPlace() - Echec (Limite 4 atteinte)")
    void createPlace_shouldThrowLimitExceeded_whenUserHas4Places() {
        when(userRepository.findByEmail("testuser")).thenReturn(Optional.of(mockUser));
        
        List<Place> fourPlaces = List.of(new Place(), new Place(), new Place(), new Place());
        when(placeRepository.findByUserId(1L)).thenReturn(fourPlaces);

        assertThatThrownBy(() -> placeService.createPlace(placeRequest, "testuser"))
                .isInstanceOf(LimitExceededException.class)
                .hasMessageContaining("limite maximale de 4 adresses");

        verify(placeRepository, never()).save(any());
    }

    @Test
    @DisplayName("getUserPlaces() - Retourne liste des lieux")
    void getUserPlaces_shouldReturnList() {
        when(userRepository.findByEmail("testuser")).thenReturn(Optional.of(mockUser));
        when(placeRepository.findByUserId(1L)).thenReturn(List.of(mockPlace));

        List<PlaceResponse> responses = placeService.getUserPlaces("testuser");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("Maison");
    }

    @Test
    @DisplayName("updatePlace() - Succes")
    void updatePlace_shouldUpdateAndReturn() {
        when(userRepository.findByEmail("testuser")).thenReturn(Optional.of(mockUser));
        when(placeRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(mockPlace));
        when(placeRepository.save(any(Place.class))).thenReturn(mockPlace);

        PlaceResponse response = placeService.updatePlace(100L, placeRequest, "testuser");

        assertThat(response.name()).isEqualTo("Nouveau Lieu");
        assertThat(mockPlace.getName()).isEqualTo("Nouveau Lieu");
    }

    @Test
    @DisplayName("updatePlace() - Echec (Lieu non trouve ou mauvais proprio)")
    void updatePlace_shouldThrowException_whenNotFoundOrNotOwned() {
        when(userRepository.findByEmail("testuser")).thenReturn(Optional.of(mockUser));
        when(placeRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> placeService.updatePlace(100L, placeRequest, "testuser"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
