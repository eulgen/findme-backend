package com.example.findme.controller;

import com.example.findme.dto.request.SigninRequest;
import com.example.findme.dto.request.SignupRequest;
import com.example.findme.dto.response.AuthResponse;
import com.example.findme.dto.response.UserResponse;
import com.example.findme.enums.Role;
import com.example.findme.exception.DuplicateResourceException;
import com.example.findme.exception.GlobalExceptionHandler;
import com.example.findme.exception.ResourceNotFoundException;
import com.example.findme.service.AuthService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests unitaires isoles pour {@link AuthController}.
 * 
 * <p>Utilise standaloneSetup car les modules d'autoconfiguration
 * comme @WebMvcTest ont ete modifies dans Spring Boot 4.x.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du AuthController")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Initialisation de MockMvc en standalone avec le GlobalExceptionHandler
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // =========================================================
    // Tests : signup
    // =========================================================

    @Test
    @DisplayName("POST /signup - Succes (201 Created)")
    void signup_shouldReturn201_whenRequestIsValid() throws Exception {
        // Arrange
        SignupRequest request = SignupRequest.builder()
                .email("test@findme.com")
                .username("testuser")
                .password("password123")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("test@findme.com")
                .username("testuser")
                .role(Role.UTILISATEUR)
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token("mock_jwt_token")
                .user(userResponse)
                .build();

        when(authService.signup(any(SignupRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock_jwt_token"))
                .andExpect(jsonPath("$.user.email").value("test@findme.com"));
    }

    @Test
    @DisplayName("POST /signup - Erreur Validation (400 Bad Request)")
    void signup_shouldReturn400_whenEmailIsInvalid() throws Exception {
        // Arrange (email invalide)
        SignupRequest request = SignupRequest.builder()
                .email("invalid-email")
                .username("testuser")
                .password("password123")
                .build();

        // Act & Assert (valide par @Valid de Spring)
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Echec de la validation des donnees de la requete"));
    }

    @Test
    @DisplayName("POST /signup - Erreur Duplication (409 Conflict)")
    void signup_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        // Arrange
        SignupRequest request = SignupRequest.builder()
                .email("duplicate@findme.com")
                .username("testuser")
                .password("password123")
                .build();

        when(authService.signup(any(SignupRequest.class)))
                .thenThrow(new DuplicateResourceException("L'adresse email est deja utilisee"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("L'adresse email est deja utilisee"));
    }

    // =========================================================
    // Tests : signin
    // =========================================================

    @Test
    @DisplayName("POST /signin - Succes (200 OK)")
    void signin_shouldReturn200_whenCredentialsAreValid() throws Exception {
        // Arrange
        SigninRequest request = SigninRequest.builder()
                .email("test@findme.com")
                .password("password123")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("test@findme.com")
                .username("testuser")
                .role(Role.UTILISATEUR)
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token("mock_jwt_token")
                .user(userResponse)
                .build();

        when(authService.signin(any(SigninRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock_jwt_token"))
                .andExpect(jsonPath("$.user.email").value("test@findme.com"));
    }

    @Test
    @DisplayName("POST /signin - Erreur Identifiants (404 Not Found)")
    void signin_shouldReturn404_whenCredentialsAreInvalid() throws Exception {
        // Arrange
        SigninRequest request = SigninRequest.builder()
                .email("test@findme.com")
                .password("wrongpassword")
                .build();

        when(authService.signin(any(SigninRequest.class)))
                .thenThrow(new ResourceNotFoundException("Identifiants incorrects"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Identifiants incorrects"));
    }
}
