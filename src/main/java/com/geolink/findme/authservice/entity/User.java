package com.geolink.findme.authservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entité JPA représentant un utilisateur de la plateforme findMe.
 * <p>
 * Cette classe est une entité métier pure : elle n'implémente PAS
 * {@code UserDetails} de Spring Security. L'adaptation vers Spring Security
 * se fait via la classe {@code UserPrincipal} (composition).
 * </p>
 * <p>
 * Le champ {@code roles} est chargé en {@code EAGER} car il est nécessaire
 * à chaque authentification pour construire les authorities du token JWT.
 * </p>
 */
@Entity
@Table(name = "users", schema = "authservice")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            schema = "authservice",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // ── Constructeurs ──

    public User() {
    }

    public User(Long id, String email, String passwordHash, String firstName,
                String lastName, AccountStatus status, Instant createdAt,
                Instant lastLoginAt, Set<Role> roles) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        if (status != null) this.status = status;
        if (createdAt != null) this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        if (roles != null) this.roles = roles;
    }

    // ── Méthodes métier ──

    /**
     * Vérifie si le compte est actif.
     */
    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    /**
     * Vérifie si l'utilisateur possède une permission donnée
     * (via l'un de ses rôles).
     *
     * @param code le code de la permission (ex. {@code USER_LIST_VIEW})
     * @return {@code true} si au moins un rôle porte cette permission
     */
    public boolean hasPermission(String code) {
        if (roles == null) return false;
        return roles.stream()
                .filter(Objects::nonNull)
                .flatMap(role -> role.getPermissions().stream())
                .filter(Objects::nonNull)
                .anyMatch(permission -> code.equalsIgnoreCase(permission.getCode()));
    }

    /**
     * Calcule le rôle principal de l'utilisateur.
     * <p>
     * Priorité : {@code ADMIN} > {@code SUPPORT_AGENT} > premier rôle trouvé.
     * Si aucun rôle, retourne {@code "USER"} par défaut.
     * C'est ce rôle unique qui est exposé dans le champ {@code role} de l'API.
     * </p>
     */
    public String primaryRole() {
        if (roles == null || roles.isEmpty()) {
            return "USER";
        }
        if (roles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()))) {
            return "ADMIN";
        }
        if (roles.stream().anyMatch(r -> "SUPPORT_AGENT".equalsIgnoreCase(r.getName()))) {
            return "SUPPORT_AGENT";
        }
        return roles.iterator().next().getName();
    }

    // ── Getters / Setters ──

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    // ── equals / hashCode sur l'email (identifiant métier) ──

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", email='" + email + "', status=" + status + "}";
    }

    // ── Builder ──

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private Long id;
        private String email;
        private String passwordHash;
        private String firstName;
        private String lastName;
        private AccountStatus status = AccountStatus.ACTIVE;
        private Instant createdAt = Instant.now();
        private Instant lastLoginAt;
        private Set<Role> roles = new HashSet<>();

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public UserBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserBuilder status(AccountStatus status) {
            this.status = status;
            return this;
        }

        public UserBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserBuilder lastLoginAt(Instant lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
            return this;
        }

        public UserBuilder roles(Set<Role> roles) {
            this.roles = roles;
            return this;
        }

        public User build() {
            return new User(id, email, passwordHash, firstName, lastName,
                    status, createdAt, lastLoginAt, roles);
        }
    }
}
