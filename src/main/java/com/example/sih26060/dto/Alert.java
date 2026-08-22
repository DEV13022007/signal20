package com.example.sih26060.dto;

import com.example.sih26060.entity.AlertCategory;
import com.example.sih26060.entity.AlertSeverity;

import java.time.Instant;

/**
 * Broadcast over STOMP (/topic/alerts) and also kept in a capped in-memory buffer so a
 * client that connects after the event still sees recent history via GET /api/alerts.
 */
public record Alert(
        long id,
        AlertSeverity severity,
        AlertCategory category,
        Long stationId,
        String stationName,
        String entityType,
        Long entityId,
        String message,
        Instant createdAt
) {
}
