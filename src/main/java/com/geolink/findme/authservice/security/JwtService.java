package com.geolink.findme.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Service utilitaire pour la génération, la validation et l'extraction des données des tokens JWT,
 * ainsi que le hachage SHA-256 des tokens opaques.
 */
@Service
public class JwtService {

    @Value("${securite.jwt.secret:dGhpc19pc19hX3Zlcnlfc2VjdXJlX2xvbmdfc2VjcmV0X2tleV9mb3Jfand0X3NpZ25pbmdfYW5kX3ZlcmlmaWNhdGlvbl8xMjM0NTY3ODkw}")
    private String secretKey;

    @Value("${securite.jwt.duree-acces-minutes:15}")
    private long accessMinutes;

    @Value("${securite.jwt.duree-rafraichissement-jours:7}")
    private long refreshDays;

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey);
        } catch (IllegalArgumentException e) {
            keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Génère un JWT d'accès signé pour l'utilisateur.
     *
     * @param userPrincipal le principal authentifié
     * @return le token JWT sous forme de String compacte
     */
    public String generateAccessToken(UserPrincipal userPrincipal) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userPrincipal.getUser().getId());
        List<String> authorities = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        extraClaims.put("authorities", authorities);
        extraClaims.put("role", userPrincipal.getUser().primaryRole());

        long now = System.currentTimeMillis();
        long exp = now + (accessMinutes * 60 * 1000);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userPrincipal.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(exp))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Génère une chaîne aléatoire opaque (UUID) pour les refresh tokens et reset tokens.
     */
    public String generateOpaqueToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Calcule l'empreinte SHA-256 d'un token opaque.
     *
     * @param token la chaîne brute du token
     * @return le hash hexadécimal SHA-256
     */
    public String hashToken(String token) {
        if (token == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithme de hachage SHA-256 introuvable", e);
        }
    }

    /**
     * Extrait l'email (sujet) d'un token JWT.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Valide un token JWT par rapport aux détails de l'utilisateur.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String email = extractEmail(token);
            return (email.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public long getAccessExpirationInSeconds() {
        return accessMinutes * 60;
    }

    public long getRefreshExpirationInDays() {
        return refreshDays;
    }
}
