package com.example.sih26060.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "stations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String code;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String country;

    private Double latitude;

    private Double longitude;

    private Integer capacity;

    @Enumerated(EnumType.STRING)
    private Season currentSeason;

    private Integer operationalSinceYear;

    @Column(nullable = false)
    private Boolean satelliteLinkActive;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (satelliteLinkActive == null) {
            satelliteLinkActive = false;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
