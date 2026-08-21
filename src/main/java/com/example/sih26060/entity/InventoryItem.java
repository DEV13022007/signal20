package com.example.sih26060.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    @JsonIgnore
    private Station station;

    /**
     * Exposed instead of the lazy {@code station} association: reading the id off an
     * uninitialized proxy is safe (no DB hit), while serializing the proxy itself is not
     * once the request-scoped session is closed (open-in-view is disabled).
     */
    @JsonProperty("stationId")
    public Long getStationId() {
        return station != null ? station.getId() : null;
    }

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer quantity;

    private String unit;

    private LocalDate expiryDate;

    @Min(0)
    private Integer minThreshold;

    @Column(nullable = false)
    private Instant lastUpdated;

    @PrePersist
    @PreUpdate
    protected void touch() {
        lastUpdated = Instant.now();
    }
}
