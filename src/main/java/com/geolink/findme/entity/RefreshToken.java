package com.geolink.findme.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Entité représentant un refresh token.
 * <p>
 * Le token brut (UUID opaque) n'est jamais stocké en base : seul le hash SHA-256
 * ({@code tokenHash}) est persisté. La rotation est assurée par le service
 * {@code RefreshTokenServiceImpl} qui révoque l'ancien token et en crée un nouveau.
 * </p>
 */
@Entity
@Table(name = "refresh_tokens", schema = "authservice")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiration;

    @Column(nullable = false)
    private boolean revoked = false;

    // ── Constructeurs ──

    public RefreshToken() {
    }

    public RefreshToken(Long id, User user, String tokenHash, Instant expiration, boolean revoked) {
        this.id = id;
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiration = expiration;
        this.revoked = revoked;
    }

    // ── Méthode métier ──

    /**
     * Un token est valide s'il n'est pas révoqué et n'est pas expiré.
     */
    public boolean isValid() {
        return !revoked && expiration != null && expiration.isAfter(Instant.now());
    }

    // ── Getters / Setters ──

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    // ── equals / hashCode sur tokenHash ──

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshToken that = (RefreshToken) o;
        return Objects.equals(tokenHash, that.tokenHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenHash);
    }

    @Override
    public String toString() {
        return "RefreshToken{id=" + id + ", revoked=" + revoked + "}";
    }

    // ── Builder ──

    public static RefreshTokenBuilder builder() {
        return new RefreshTokenBuilder();
    }

    public static class RefreshTokenBuilder {
        private Long id;
        private User user;
        private String tokenHash;
        private Instant expiration;
        private boolean revoked = false;

        public RefreshTokenBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public RefreshTokenBuilder user(User user) {
            this.user = user;
            return this;
        }

        public RefreshTokenBuilder tokenHash(String tokenHash) {
            this.tokenHash = tokenHash;
            return this;
        }

        public RefreshTokenBuilder expiration(Instant expiration) {
            this.expiration = expiration;
            return this;
        }

        public RefreshTokenBuilder revoked(boolean revoked) {
            this.revoked = revoked;
            return this;
        }

        public RefreshToken build() {
            return new RefreshToken(id, user, tokenHash, expiration, revoked);
        }
    }
}
