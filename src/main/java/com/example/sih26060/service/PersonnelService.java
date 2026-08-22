package com.example.sih26060.service;

import com.example.sih26060.entity.Personnel;
import com.example.sih26060.entity.Priority;
import com.example.sih26060.entity.Station;
import com.example.sih26060.entity.SyncOperation;
import com.example.sih26060.repository.PersonnelRepository;
import com.example.sih26060.repository.StationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every mutation is mirrored into the offline-first sync queue. A change to healthStatus
 * is enqueued at MEDICAL priority since it reflects a crew member's condition; every other
 * change (roster, rotation dates, onboarding/offboarding) is ROUTINE.
 */
@Service
@RequiredArgsConstructor
public class PersonnelService {

    private final PersonnelRepository personnelRepository;
    private final StationRepository stationRepository;
    private final SyncQueueService syncQueueService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Personnel create(Personnel person, Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new EntityNotFoundException("Station not found: " + stationId));
        person.setId(null);
        person.setStation(station);
        Personnel saved = personnelRepository.save(person);
        enqueueSync(saved, SyncOperation.CREATE, Priority.ROUTINE);
        return saved;
    }

    @Transactional
    public Personnel update(Long id, Personnel updates) {
        Personnel existing = personnelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Personnel not found: " + id));
        boolean healthChanged = existing.getHealthStatus() != updates.getHealthStatus();

        existing.setName(updates.getName());
        existing.setRole(updates.getRole());
        existing.setRotationStart(updates.getRotationStart());
        existing.setRotationEnd(updates.getRotationEnd());
        existing.setHealthStatus(updates.getHealthStatus());
        Personnel saved = personnelRepository.save(existing);

        enqueueSync(saved, SyncOperation.UPDATE, healthChanged ? Priority.MEDICAL : Priority.ROUTINE);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Personnel existing = personnelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Personnel not found: " + id));
        personnelRepository.deleteById(id);
        enqueueSync(existing, SyncOperation.DELETE, Priority.ROUTINE);
    }

    private void enqueueSync(Personnel person, SyncOperation operation, Priority priority) {
        syncQueueService.enqueue(person.getStation(), "Personnel", person.getId(), operation,
                priority, toPayload(person));
    }

    private String toPayload(Personnel person) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", person.getId());
        snapshot.put("stationId", person.getStation().getId());
        snapshot.put("name", person.getName());
        snapshot.put("role", person.getRole());
        snapshot.put("rotationStart", person.getRotationStart());
        snapshot.put("rotationEnd", person.getRotationEnd());
        snapshot.put("healthStatus", person.getHealthStatus());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize personnel " + person.getId(), e);
        }
    }
}
