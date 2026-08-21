package com.example.sih26060.dto;

import com.example.sih26060.entity.Priority;

import java.util.List;
import java.util.Map;

public record SyncOverallStatus(
        long totalPending,
        long totalSynced,
        Map<Priority, Long> pendingByPriority,
        List<StationLinkStatus> stations
) {
}
