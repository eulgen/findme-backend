package com.example.findme.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entite JPA representant un lieu enregistre par un utilisateur.
 *
 * @author findme-team
 */
@Entity
@Table(name = "places")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 50)
    private String type; // ex: restaurant, domicile, travail

    /**
     * Un lieu appartient a un seul utilisateur,
     * un utilisateur peut avoir plusieurs lieux.
     * Le FetchType.LAZY permet de ne pas charger l'utilisateur
     * si l'on a juste besoin des infos du lieu.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @org.hibernate.annotations.CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
