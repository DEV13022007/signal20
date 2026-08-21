package com.example.sih26060.controller;

import com.example.sih26060.dto.StationSyncStatus;
import com.example.sih26060.dto.SyncOverallStatus;
import com.example.sih26060.entity.SyncRecord;
import com.example.sih26060.entity.SyncStatus;
import com.example.sih26060.repository.SyncRecordRepository;
import com.example.sih26060.service.SyncQueueService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Tag(name = "Sync")
public class SyncController {

    private final SyncQueueService syncQueueService;
    private final SyncRecordRepository syncRecordRepository;

    @GetMapping("/status")
    public SyncOverallStatus getOverallStatus() {
        return syncQueueService.getOverallStatus();
    }

    @GetMapping("/status/{stationId}")
    public StationSyncStatus getStationStatus(@PathVariable Long stationId) {
        return syncQueueService.getStationStatus(stationId);
    }

    @GetMapping("/records")
    public List<SyncRecord> getRecords(@RequestParam(required = false) SyncStatus status) {
        return status != null
                ? syncRecordRepository.findByStatus(status)
                : syncRecordRepository.findAll();
    }

    @PostMapping("/stations/{stationId}/flush")
    public Map<String, Object> flush(@PathVariable Long stationId) {
        int pushed = syncQueueService.flushStation(stationId);
        return Map.of("stationId", stationId, "pushed", pushed);
    }
}
