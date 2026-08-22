package com.example.sih26060.service;

import com.example.sih26060.entity.Equipment;
import com.example.sih26060.entity.Priority;
import com.example.sih26060.entity.Station;
import com.example.sih26060.entity.SyncOperation;
import com.example.sih26060.repository.EquipmentRepository;
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
 * Every mutation is mirrored into the offline-first sync queue at EQUIPMENT priority —
 * below MEDICAL but above SUPPLY/ROUTINE, since a failed generator or vehicle affects the
 * whole station rather than a single person or supply line.
 */
@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final StationRepository stationRepository;
    private final SyncQueueService syncQueueService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Equipment create(Equipment equipment, Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new EntityNotFoundException("Station not found: " + stationId));
        equipment.setId(null);
        equipment.setStation(station);
        Equipment saved = equipmentRepository.save(equipment);
        enqueueSync(saved, SyncOperation.CREATE);
        return saved;
    }

    @Transactional
    public Equipment update(Long id, Equipment updates) {
        Equipment existing = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Equipment not found: " + id));
        existing.setName(updates.getName());
        existing.setType(updates.getType());
        existing.setStatus(updates.getStatus());
        existing.setLastServiceDate(updates.getLastServiceDate());
        existing.setNextServiceDue(updates.getNextServiceDue());
        Equipment saved = equipmentRepository.save(existing);
        enqueueSync(saved, SyncOperation.UPDATE);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Equipment existing = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Equipment not found: " + id));
        equipmentRepository.deleteById(id);
        enqueueSync(existing, SyncOperation.DELETE);
    }

    private void enqueueSync(Equipment equipment, SyncOperation operation) {
        syncQueueService.enqueue(equipment.getStation(), "Equipment", equipment.getId(), operation,
                Priority.EQUIPMENT, toPayload(equipment));
    }

    private String toPayload(Equipment equipment) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", equipment.getId());
        snapshot.put("stationId", equipment.getStation().getId());
        snapshot.put("name", equipment.getName());
        snapshot.put("type", equipment.getType());
        snapshot.put("status", equipment.getStatus());
        snapshot.put("lastServiceDate", equipment.getLastServiceDate());
        snapshot.put("nextServiceDue", equipment.getNextServiceDue());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize equipment " + equipment.getId(), e);
        }
    }
}
