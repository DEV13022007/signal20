package com.example.sih26060.service;

import com.example.sih26060.entity.AlertCategory;
import com.example.sih26060.entity.AlertSeverity;
import com.example.sih26060.entity.InventoryItem;
import com.example.sih26060.entity.Station;
import com.example.sih26060.entity.SyncOperation;
import com.example.sih26060.repository.InventoryItemRepository;
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
 * Every mutation is mirrored into the offline-first sync queue so it can be pushed to
 * the mainland the next time the owning station's satellite link comes up.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final StationRepository stationRepository;
    private final SyncQueueService syncQueueService;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    @Transactional
    public InventoryItem create(InventoryItem item, Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new EntityNotFoundException("Station not found: " + stationId));
        item.setId(null);
        item.setStation(station);
        InventoryItem saved = inventoryItemRepository.save(item);
        enqueueSync(saved, SyncOperation.CREATE);
        if (isBelowThreshold(saved)) {
            raiseLowStockAlert(saved);
        }
        return saved;
    }

    @Transactional
    public InventoryItem update(Long id, InventoryItem updates) {
        InventoryItem existing = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found: " + id));
        boolean wasBelowThreshold = isBelowThreshold(existing);

        existing.setName(updates.getName());
        existing.setPriority(updates.getPriority());
        existing.setQuantity(updates.getQuantity());
        existing.setUnit(updates.getUnit());
        existing.setExpiryDate(updates.getExpiryDate());
        existing.setMinThreshold(updates.getMinThreshold());
        InventoryItem saved = inventoryItemRepository.save(existing);
        enqueueSync(saved, SyncOperation.UPDATE);

        if (!wasBelowThreshold && isBelowThreshold(saved)) {
            raiseLowStockAlert(saved);
        }
        return saved;
    }

    private boolean isBelowThreshold(InventoryItem item) {
        return item.getMinThreshold() != null && item.getQuantity() != null
                && item.getQuantity() <= item.getMinThreshold();
    }

    private void raiseLowStockAlert(InventoryItem item) {
        alertService.raise(AlertSeverity.WARNING, AlertCategory.LOW_STOCK, item.getStation(),
                "InventoryItem", item.getId(),
                "%s at %s dropped to %d (threshold %d)".formatted(
                        item.getName(), item.getStation().getName(), item.getQuantity(), item.getMinThreshold()));
    }

    @Transactional
    public void delete(Long id) {
        InventoryItem existing = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found: " + id));
        inventoryItemRepository.deleteById(id);
        enqueueSync(existing, SyncOperation.DELETE);
    }

    private void enqueueSync(InventoryItem item, SyncOperation operation) {
        syncQueueService.enqueue(item.getStation(), "InventoryItem", item.getId(), operation,
                item.getPriority(), toPayload(item));
    }

    private String toPayload(InventoryItem item) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", item.getId());
        snapshot.put("stationId", item.getStation().getId());
        snapshot.put("name", item.getName());
        snapshot.put("priority", item.getPriority());
        snapshot.put("quantity", item.getQuantity());
        snapshot.put("unit", item.getUnit());
        snapshot.put("expiryDate", item.getExpiryDate());
        snapshot.put("minThreshold", item.getMinThreshold());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize inventory item " + item.getId(), e);
        }
    }
}
