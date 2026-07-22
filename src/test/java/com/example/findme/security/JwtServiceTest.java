package com.example.findme.security;

import com.example.findme.entity.User;
import com.example.findme.enums.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires pour le {@link JwtService}.
 * <p>Aucun contexte Spring n'est charge, on injecte les proprietes via ReflectionTestUtils.</p>
 */
@DisplayName("Tests du JwtService")
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;
    
    // Cle secrete Base64 d'au moins 256 bits (32 octets) pour HMAC-SHA256
    private final String testSecret = "maCleSecreteTresLonguePourLesTestsQuiDoitFaireAuMoins256Bits";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Injection manuelle des @Value
        ReflectionTestUtils.setField(jwtService, "secretKey", java.util.Base64.getEncoder().encodeToString(testSecret.getBytes()));
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 1000 * 60 * 60); // 1 heure

        testUser = User.builder()
                .email("test@findme.com")
                .username("testuser")
                .role(Role.UTILISATEUR)
                .build();
    }

    @Test
    @DisplayName("generateToken() doit creer un token non nul")
    void generateToken_shouldReturnValidToken() {
        String token = jwtService.generateToken(testUser);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("extractUsername() doit extraire le bon email")
    void extractUsername_shouldReturnEmail() {
        String token = jwtService.generateToken(testUser);
        String extractedEmail = jwtService.extractUsername(token);
        
        assertThat(extractedEmail).isEqualTo("test@findme.com");
    }

    @Test
    @DisplayName("isTokenValid() doit retourner true pour le bon utilisateur")
    void isTokenValid_shouldReturnTrue_forCorrectUser() {
        String token = jwtService.generateToken(testUser);
        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() doit retourner false pour un mauvais utilisateur")
    void isTokenValid_shouldReturnFalse_forWrongUser() {
        String token = jwtService.generateToken(testUser);
        
        User wrongUser = User.builder().email("wrong@findme.com").build();
        assertThat(jwtService.isTokenValid(token, wrongUser)).isFalse();
    }

    @Test
    @DisplayName("Un token expire doit lever ExpiredJwtException")
    void expiredToken_shouldThrowException() {
        // Pour ce test, on cree un token manuellement avec une expiration dans le passe
        byte[] keyBytes = testSecret.getBytes();
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        
        String expiredToken = Jwts.builder()
                .subject(testUser.getEmail())
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 1000)) // expire il y a 1 seconde
                .signWith(key, Jwts.SIG.HS256)
                .compact();
                
        assertThatThrownBy(() -> jwtService.extractUsername(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
