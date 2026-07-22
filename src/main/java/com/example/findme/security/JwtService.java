package com.example.findme.security;

import com.example.findme.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service dedie a la generation et a la validation des tokens JWT (JSON Web Token).
 *
 * <p>Responsabilites :</p>
 * <ul>
 *   <li>Generer un token valide pour un utilisateur authentifie.</li>
 *   <li>Extraire des informations (claims) d'un token recu.</li>
 *   <li>Valider la signature cryptographique et l'expiration du token.</li>
 * </ul>
 *
 * @author findme-team
 */
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Genere un token JWT pour l'utilisateur fourni.
     * <p>Inclut le role de l'utilisateur en tant que "claim" supplementaire.</p>
     *
     * @param user l'utilisateur pour lequel generer le token
     * @return le token JWT sous forme de chaine de caracteres
     */
    public String generateToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        
        return buildToken(extraClaims, user.getEmail(), jwtExpirationMs);
    }

    /**
     * Extrait l'adresse email (Subject) contenue dans le token JWT.
     *
     * @param token le token JWT
     * @return l'adresse email (username)
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Verifie si le token est valide pour l'utilisateur donne.
     * <p>Le token est valide si l'email correspond et s'il n'est pas expire.</p>
     *
     * @param token le token JWT
     * @param user  l'utilisateur compare
     * @return true si le token est valide, false sinon
     */
    public boolean isTokenValid(String token, User user) {
        final String username = extractUsername(token);
        return (username.equals(user.getEmail())) && !isTokenExpired(token);
    }

    // =========================================================
    // Methodes internes (privees)
    // =========================================================

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
