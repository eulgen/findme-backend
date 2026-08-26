package com.geolink.findme.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Entité représentant une permission unitaire du système RBAC.
 * <p>
 * Chaque permission est identifiée par un {@code code} unique
 * (ex. {@code USER_LIST_VIEW}, {@code SUPPORT_TICKET_MANAGE}).
 * Les permissions sont associées aux rôles via la table de jointure {@code role_permissions}.
 * </p>
 */
@Entity
@Table(name = "permissions", schema = "authservice")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(length = 255)
    private String description;

    // ── Constructeurs ──

    public Permission() {
    }

    public Permission(Long id, String code, String description) {
        this.id = id;
        this.code = code;
        this.description = description;
    }

    // ── Getters / Setters ──

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // ── equals / hashCode sur le code (identifiant métier) ──

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "Permission{id=" + id + ", code='" + code + "'}";
    }

    // ── Builder ──

    public static PermissionBuilder builder() {
        return new PermissionBuilder();
    }

    public static class PermissionBuilder {
        private Long id;
        private String code;
        private String description;

        public PermissionBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PermissionBuilder code(String code) {
            this.code = code;
            return this;
        }

        public PermissionBuilder description(String description) {
            this.description = description;
            return this;
        }

        public Permission build() {
            return new Permission(id, code, description);
        }
    }
}
