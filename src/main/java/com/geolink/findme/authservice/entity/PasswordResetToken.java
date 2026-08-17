package com.geolink.findme.authservice.entity;

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
 * Entité représentant un token de réinitialisation de mot de passe.
 * <p>
 * Comme pour les refresh tokens, seul le hash SHA-256 est stocké en base.
 * Le token brut est envoyé à l'utilisateur (par email ou autre canal),
 * puis hashé côté serveur pour la vérification.
 * </p>
 */
@Entity
@Table(name = "password_reset_tokens", schema = "authservice")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean used = false;

    // ── Constructeurs ──

    public PasswordResetToken() {
    }

    public PasswordResetToken(Long id, User user, String tokenHash,
                              Instant expiryDate, boolean used) {
        this.id = id;
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiryDate = expiryDate;
        this.used = used;
    }

    // ── Méthodes métier ──

    /**
     * Vérifie si le token est expiré.
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(Instant.now());
    }

    /**
     * Marque le token comme utilisé (consommé).
     */
    public void markUsed() {
        this.used = true;
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

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    // ── equals / hashCode sur tokenHash ──

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasswordResetToken that = (PasswordResetToken) o;
        return Objects.equals(tokenHash, that.tokenHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenHash);
    }

    @Override
    public String toString() {
        return "PasswordResetToken{id=" + id + ", used=" + used + "}";
    }

    // ── Builder ──

    public static PasswordResetTokenBuilder builder() {
        return new PasswordResetTokenBuilder();
    }

    public static class PasswordResetTokenBuilder {
        private Long id;
        private User user;
        private String tokenHash;
        private Instant expiryDate;
        private boolean used = false;

        public PasswordResetTokenBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PasswordResetTokenBuilder user(User user) {
            this.user = user;
            return this;
        }

        public PasswordResetTokenBuilder tokenHash(String tokenHash) {
            this.tokenHash = tokenHash;
            return this;
        }

        public PasswordResetTokenBuilder expiryDate(Instant expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public PasswordResetTokenBuilder used(boolean used) {
            this.used = used;
            return this;
        }

        public PasswordResetToken build() {
            return new PasswordResetToken(id, user, tokenHash, expiryDate, used);
        }
    }
}
