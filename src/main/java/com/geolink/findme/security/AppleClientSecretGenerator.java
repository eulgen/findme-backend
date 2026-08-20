package com.geolink.findme.security;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

/**
 * Composant générant dynamiquement le client_secret JWT pour Apple Sign-In
 * signé en ES256 (EC private key).
 */
@Component
@Slf4j
public class AppleClientSecretGenerator {

    @Value("${apple.team-id:mock-team-id}")
    private String teamId;

    @Value("${apple.client-id:com.geolink.findme.client}")
    private String clientId;

    @Value("${apple.key-id:mock-key-id}")
    private String keyId;

    @Value("${apple.private-key-path:}")
    private String privateKeyPath;

    public String generate() {
        try {
            if (privateKeyPath == null || privateKeyPath.isBlank() || !Files.exists(Path.of(privateKeyPath))) {
                log.warn("Fichier de clé privée Apple introuvable ({}), utilisation du secret mock.", privateKeyPath);
                return "mock-apple-client-secret";
            }

            PrivateKey privateKey = loadEcPrivateKey(privateKeyPath);
            Instant now = Instant.now();

            return Jwts.builder()
                    .header().add("kid", keyId).and()
                    .issuer(teamId)
                    .subject(clientId)
                    .audience().add("https://appleid.apple.com").and()
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plus(150, ChronoUnit.DAYS)))
                    .signWith(privateKey, Jwts.SIG.ES256)
                    .compact();
        } catch (Exception e) {
            log.error("Impossible de générer le client_secret Apple JWT: {}", e.getMessage());
            return "mock-apple-client-secret";
        }
    }

    private PrivateKey loadEcPrivateKey(String path) throws Exception {
        String pem = Files.readString(Path.of(path))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }
}
