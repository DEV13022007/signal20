package com.example.sih26060.service;

import com.example.sih26060.dto.StationLinkStatus;
import com.example.sih26060.dto.StationSyncStatus;
import com.example.sih26060.dto.SyncOverallStatus;
import com.example.sih26060.entity.*;
import com.example.sih26060.repository.StationRepository;
import com.example.sih26060.repository.SyncRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Offline-first sync queue: SyncRecords are persisted as soon as they're created, so
 * pending work survives restarts while a station has no satellite link. As soon as a
 * station's link comes up, its pending records are pushed in priority-ordered batches
 * (MEDICAL > EQUIPMENT > SUPPLY > ROUTINE).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncQueueService {

    private static final int BATCH_SIZE = 20;

    private final SyncRecordRepository syncRecordRepository;
    private final StationRepository stationRepository;

    @Transactional
    public SyncRecord enqueue(Station station, String entityType, Long entityId,
                               SyncOperation operation, Priority priority, String payload) {
        SyncRecord record = SyncRecord.builder()
                .station(station)
                .entityType(entityType)
                .entityId(entityId)
                .operation(operation)
                .priority(priority)
                .payload(payload)
                .build();
        record = syncRecordRepository.save(record);

        if (Boolean.TRUE.equals(station.getSatelliteLinkActive())) {
            flushStation(station.getId());
        }
        return record;
    }

    /**
     * Pushes all pending records for a station, batched by priority order.
     * Returns the number of records pushed.
     */
    @Transactional
    public int flushStation(Long stationId) {
        List<SyncRecord> pending = syncRecordRepository.findByStation_IdAndStatus(stationId, SyncStatus.PENDING);
        List<SyncRecord> ordered = pending.stream()
                .sorted(Comparator.comparingInt(r -> r.getPriority().ordinal()))
                .toList();

        int pushed = 0;
        for (List<SyncRecord> batch : partition(ordered, BATCH_SIZE)) {
            pushBatch(stationId, batch);
            pushed += batch.size();
        }
        return pushed;
    }

    private void pushBatch(Long stationId, List<SyncRecord> batch) {
        Instant now = Instant.now();
        batch.forEach(r -> {
            r.setStatus(SyncStatus.SYNCED);
            r.setSyncedAt(now);
        });
        syncRecordRepository.saveAll(batch);
        log.info("Pushed batch of {} sync record(s) [{}] for station {}", batch.size(),
                batch.isEmpty() ? "" : batch.get(0).getPriority(), stationId);
    }

    /**
     * Safety net: re-checks stations whose link is already active in case records were
     * queued between link-up and this sweep (e.g. concurrent writes).
     */
    @Scheduled(fixedDelayString = "${polarconnect.sync.sweep-interval-ms:30000}")
    @Transactional
    public void sweepActiveLinks() {
        for (Station station : stationRepository.findBySatelliteLinkActiveTrue()) {
            flushStation(station.getId());
        }
    }

    @Transactional(readOnly = true)
    public SyncOverallStatus getOverallStatus() {
        return getOverallStatus(null);
    }

    /**
     * When scopedStationId is non-null (a STATION_MANAGER/CREW caller), every figure is
     * narrowed to that one station instead of the whole network.
     */
    @Transactional(readOnly = true)
    public SyncOverallStatus getOverallStatus(Long scopedStationId) {
        List<SyncRecord> pending = scopedStationId != null
                ? syncRecordRepository.findByStation_IdAndStatus(scopedStationId, SyncStatus.PENDING)
                : syncRecordRepository.findByStatus(SyncStatus.PENDING);
        Map<Priority, Long> byPriority = countByPriority(pending);
        long totalSynced = scopedStationId != null
                ? syncRecordRepository.countByStation_IdAndStatus(scopedStationId, SyncStatus.SYNCED)
                : syncRecordRepository.countByStatus(SyncStatus.SYNCED);

        List<Station> stationList = scopedStationId != null
                ? stationRepository.findById(scopedStationId).map(List::of).orElseGet(List::of)
                : stationRepository.findAll();
        List<StationLinkStatus> stations = stationList.stream()
                .map(s -> new StationLinkStatus(
                        s.getId(),
                        s.getName(),
                        Boolean.TRUE.equals(s.getSatelliteLinkActive()),
                        pending.stream().filter(r -> r.getStation().getId().equals(s.getId())).count()))
                .toList();

        return new SyncOverallStatus(pending.size(), totalSynced, byPriority, stations);
    }

    @Transactional(readOnly = true)
    public StationSyncStatus getStationStatus(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new EntityNotFoundException("Station not found: " + stationId));
        List<SyncRecord> pending = syncRecordRepository.findByStation_IdAndStatus(stationId, SyncStatus.PENDING);
        return new StationSyncStatus(
                stationId,
                station.getName(),
                Boolean.TRUE.equals(station.getSatelliteLinkActive()),
                pending.size(),
                countByPriority(pending));
    }

    private Map<Priority, Long> countByPriority(List<SyncRecord> records) {
        Map<Priority, Long> byPriority = new EnumMap<>(Priority.class);
        for (Priority priority : Priority.values()) {
            byPriority.put(priority, records.stream().filter(r -> r.getPriority() == priority).count());
        }
        return byPriority;
    }

    private static <T> List<List<T>> partition(List<T> items, int size) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += size) {
            batches.add(items.subList(i, Math.min(i + size, items.size())));
        }
        return batches;
    }
}
