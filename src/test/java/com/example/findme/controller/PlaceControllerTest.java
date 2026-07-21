package com.example.findme.controller;

import com.example.findme.dto.request.PlaceRequest;
import com.example.findme.dto.response.PlaceResponse;
import com.example.findme.exception.GlobalExceptionHandler;
import com.example.findme.exception.LimitExceededException;
import com.example.findme.exception.ResourceNotFoundException;
import com.example.findme.service.PlaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du PlaceController")
class PlaceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PlaceService placeService;

    @InjectMocks
    private PlaceController placeController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Principal mockPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(placeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
                
        mockPrincipal = mock(Principal.class);
        lenient().when(mockPrincipal.getName()).thenReturn("test@findme.com");
    }

    @Test
    @DisplayName("POST /api/v1/places - Succes (201)")
    void createPlace_shouldReturn201() throws Exception {
        PlaceRequest request = PlaceRequest.builder().name("Gym").address("1 rue de sport").build();
        PlaceResponse response = PlaceResponse.builder()
                .id(1L).name("Gym").address("1 rue de sport").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(placeService.createPlace(any(PlaceRequest.class), eq("test@findme.com"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/places")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Gym"));
    }

    @Test
    @DisplayName("POST /api/v1/places - Validation Error (400)")
    void createPlace_shouldReturn400_whenNameIsBlank() throws Exception {
        PlaceRequest request = PlaceRequest.builder().address("1 rue").build();

        mockMvc.perform(post("/api/v1/places")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Echec de la validation des donnees de la requete"));
    }
    
    @Test
    @DisplayName("POST /api/v1/places - Limite Atteinte (400)")
    void createPlace_shouldReturn400_whenLimitExceeded() throws Exception {
        PlaceRequest request = PlaceRequest.builder().name("Gym").address("1 rue").build();

        when(placeService.createPlace(any(), eq("test@findme.com")))
                .thenThrow(new LimitExceededException("Limite max atteinte"));

        mockMvc.perform(post("/api/v1/places")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Limite max atteinte"));
    }

    @Test
    @DisplayName("GET /api/v1/places - Succes (200)")
    void getUserPlaces_shouldReturn200() throws Exception {
        PlaceResponse response = PlaceResponse.builder().id(1L).name("Maison").build();
        when(placeService.getUserPlaces("test@findme.com")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/places")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Maison"));
    }

    @Test
    @DisplayName("GET /api/v1/places/{id} - Succes (200)")
    void getPlaceById_shouldReturn200() throws Exception {
        PlaceResponse response = PlaceResponse.builder().id(10L).name("Travail").build();
        when(placeService.getPlaceById(10L, "test@findme.com")).thenReturn(response);

        mockMvc.perform(get("/api/v1/places/10")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Travail"));
    }

    @Test
    @DisplayName("GET /api/v1/places/{id} - Introuvable (404)")
    void getPlaceById_shouldReturn404_whenNotFound() throws Exception {
        when(placeService.getPlaceById(99L, "test@findme.com"))
                .thenThrow(new ResourceNotFoundException("Lieu introuvable"));

        mockMvc.perform(get("/api/v1/places/99")
                        .principal(mockPrincipal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/v1/places/{id} - Succes (200)")
    void updatePlace_shouldReturn200() throws Exception {
        PlaceRequest request = PlaceRequest.builder().name("Gym Updated").address("1 rue").build();
        PlaceResponse response = PlaceResponse.builder().id(1L).name("Gym Updated").build();

        when(placeService.updatePlace(eq(1L), any(PlaceRequest.class), eq("test@findme.com"))).thenReturn(response);

        mockMvc.perform(put("/api/v1/places/1")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gym Updated"));
    }

    @Test
    @DisplayName("DELETE /api/v1/places/{id} - Succes (204)")
    void deletePlace_shouldReturn204() throws Exception {
        doNothing().when(placeService).deletePlace(1L, "test@findme.com");

        mockMvc.perform(delete("/api/v1/places/1")
                        .principal(mockPrincipal))
                .andExpect(status().isNoContent());

        verify(placeService, times(1)).deletePlace(1L, "test@findme.com");
    }
}
