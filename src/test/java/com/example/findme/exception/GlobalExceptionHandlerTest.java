package com.example.findme.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests unitaires pour le {@link GlobalExceptionHandler}.
 * <p>
 * Utilise {@code MockMvcBuilders.standaloneSetup} pour tester le ControllerAdvice
 * en isolation, evitant les problemes lies aux changements de Spring Boot 4.x
 * sur les annotations @WebMvcTest.
 * </p>
 *
 * @author findme-team
 */
@DisplayName("Tests du GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Initialisation de MockMvc en isolation avec le DummyController et le GlobalExceptionHandler
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // =========================================================
    // DUMMY CONTROLLER POUR DECLENCHER LES EXCEPTIONS
    // =========================================================

    @RestController
    @RequestMapping("/test/exceptions")
    static class DummyController {

        @GetMapping("/not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("Ressource introuvable test");
        }

        @GetMapping("/duplicate")
        public void throwDuplicate() {
            throw new DuplicateResourceException("Ressource en conflit test");
        }

        @GetMapping("/generic")
        public void throwGeneric() {
            throw new RuntimeException("Erreur generique test");
        }

        @PostMapping("/validation")
        public void throwValidation(@Valid @RequestBody DummyDto dto) {
            // Fait rien, l'exception est levee par @Valid avant
        }

        static class DummyDto {
            @NotBlank(message = "Le nom ne doit pas etre vide")
            private String name;

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
        }
    }

    // =========================================================
    // TESTS
    // =========================================================

    @Test
    @DisplayName("ResourceNotFoundException doit renvoyer 404 avec ApiErrorResponse")
    void shouldHandleResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/test/exceptions/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ressource introuvable test"))
                .andExpect(jsonPath("$.path").value("/test/exceptions/not-found"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.validationErrors").doesNotExist());
    }

    @Test
    @DisplayName("DuplicateResourceException doit renvoyer 409 avec ApiErrorResponse")
    void shouldHandleDuplicateResourceException() throws Exception {
        mockMvc.perform(get("/test/exceptions/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Ressource en conflit test"))
                .andExpect(jsonPath("$.path").value("/test/exceptions/duplicate"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Exception generique doit renvoyer 500 avec message securise")
    void shouldHandleGenericException() throws Exception {
        mockMvc.perform(get("/test/exceptions/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Une erreur inattendue s'est produite sur le serveur"))
                .andExpect(jsonPath("$.path").value("/test/exceptions/generic"));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException doit renvoyer 400 avec details des champs")
    void shouldHandleValidationException() throws Exception {
        // Envoi d'un objet vide sans "name"
        String json = "{}";

        mockMvc.perform(post("/test/exceptions/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Echec de la validation des donnees de la requete"))
                .andExpect(jsonPath("$.validationErrors.name").value("Le nom ne doit pas etre vide"));
    }
}
