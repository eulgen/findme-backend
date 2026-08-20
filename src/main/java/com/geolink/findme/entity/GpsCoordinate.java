package com.geolink.findme.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Entité JPA représentant les coordonnées GPS (Latitude / Longitude) associées à une adresse.
 */
@Entity
@Table(name = "gps_coordinates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpsCoordinate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GpsCoordinate that = (GpsCoordinate) o;
        return Objects.equals(id, that.id) ||
                (Objects.equals(latitude, that.latitude) && Objects.equals(longitude, that.longitude));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, latitude, longitude);
    }

    @Override
    public String toString() {
        return "GpsCoordinate{id=" + id + ", latitude=" + latitude + ", longitude=" + longitude + "}";
    }
}
