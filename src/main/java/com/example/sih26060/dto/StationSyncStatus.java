package com.example.sih26060.dto;

import com.example.sih26060.entity.Priority;

import java.util.Map;

public record StationSyncStatus(
        Long stationId,
        String stationName,
        boolean satelliteLinkActive,
        long pendingCount,
        Map<Priority, Long> pendingByPriority
) {
}
