package com.example.sih26060.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "sync_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    @JsonIgnore
    private Station station;

    @JsonProperty("stationId")
    public Long getStationId() {
        return station != null ? station.getId() : null;
    }

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncOperation operation;

    /**
     * Drives batching order in the sync queue: MEDICAL > EQUIPMENT > SUPPLY > ROUTINE.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    // Plain text column, not @Lob: on Postgres, @Lob maps String to a true Large Object
    // (oid) which can only be streamed inside the exact transaction that opened it — a
    // derived query like findByStation_Id reads it via a separate path that Postgres
    // rejects with "Large Objects may not be used in auto-commit mode". The payload is
    // just a small JSON snapshot, so a plain text column avoids LO semantics entirely.
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant syncedAt;

    @Column(nullable = false)
    private Integer retryCount;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = SyncStatus.PENDING;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }
}
