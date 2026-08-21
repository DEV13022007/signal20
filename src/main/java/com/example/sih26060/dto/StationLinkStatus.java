package com.example.sih26060.dto;

public record StationLinkStatus(
        Long stationId,
        String stationName,
        boolean satelliteLinkActive,
        long pendingCount
) {
}
