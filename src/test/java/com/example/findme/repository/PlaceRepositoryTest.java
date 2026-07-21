package com.example.findme.repository;

import com.example.findme.entity.Place;
import com.example.findme.entity.User;
import com.example.findme.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'integration du repository {@link PlaceRepository}.
 *
 * <p>Utilise {@code @SpringBootTest} et {@code @Transactional} pour
 * se connecter a la base H2 en memoire (profil 'test') 
 * et isoler chaque test.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("Tests du PlaceRepository")
class PlaceRepositoryTest {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private Place place1;
    private Place place2;

    @BeforeEach
    void setUp() {
        // Creer les utilisateurs
        testUser1 = userRepository.save(User.builder()
                .email("user1@findme.com").username("user1").password("pwd")
                .role(Role.UTILISATEUR).build());
                
        testUser2 = userRepository.save(User.builder()
                .email("user2@findme.com").username("user2").password("pwd")
                .role(Role.UTILISATEUR).build());

        // Creer les lieux
        place1 = placeRepository.save(Place.builder()
                .name("Maison")
                .address("123 Rue de Paris")
                .latitude(48.8566)
                .longitude(2.3522)
                .type("domicile")
                .user(testUser1)
                .build());

        place2 = placeRepository.save(Place.builder()
                .name("Travail")
                .address("456 Avenue de Lyon")
                .user(testUser1)
                .build());
                
        placeRepository.save(Place.builder()
                .name("Gym")
                .address("789 Boulevard de Marseille")
                .user(testUser2)
                .build());
    }

    @Test
    @DisplayName("findByUserId() doit retourner uniquement les lieux de l'utilisateur")
    void findByUserId_shouldReturnUserPlaces() {
        List<Place> user1Places = placeRepository.findByUserId(testUser1.getId());
        List<Place> user2Places = placeRepository.findByUserId(testUser2.getId());

        assertThat(user1Places).hasSize(2);
        assertThat(user1Places).extracting(Place::getName).containsExactlyInAnyOrder("Maison", "Travail");
        
        assertThat(user2Places).hasSize(1);
        assertThat(user2Places.get(0).getName()).isEqualTo("Gym");
    }

    @Test
    @DisplayName("findByIdAndUserId() doit retourner le lieu si le proprietaire est correct")
    void findByIdAndUserId_shouldReturnPlace_whenOwnedByUser() {
        Optional<Place> found = placeRepository.findByIdAndUserId(place1.getId(), testUser1.getId());
        
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Maison");
    }

    @Test
    @DisplayName("findByIdAndUserId() doit etre vide si le lieu appartient a un autre utilisateur")
    void findByIdAndUserId_shouldReturnEmpty_whenOwnedByAnotherUser() {
        Optional<Place> found = placeRepository.findByIdAndUserId(place1.getId(), testUser2.getId());
        
        assertThat(found).isEmpty();
    }
}
