package com.example.sih26060.service;

import com.example.sih26060.entity.Station;
import com.example.sih26060.repository.StationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StationService {

    private final StationRepository stationRepository;
    private final SyncQueueService syncQueueService;

    @Transactional
    public Station setSatelliteLink(Long stationId, boolean active) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new EntityNotFoundException("Station not found: " + stationId));
        boolean wasActive = Boolean.TRUE.equals(station.getSatelliteLinkActive());
        station.setSatelliteLinkActive(active);
        station = stationRepository.save(station);

        if (active && !wasActive) {
            syncQueueService.flushStation(stationId);
        }
        return station;
    }
}
