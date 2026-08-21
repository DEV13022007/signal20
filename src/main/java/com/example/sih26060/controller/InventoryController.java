package com.example.sih26060.controller;

import com.example.sih26060.entity.InventoryItem;
import com.example.sih26060.repository.InventoryItemRepository;
import com.example.sih26060.service.InventoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory")
public class InventoryController {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryService inventoryService;

    @GetMapping
    public List<InventoryItem> getAll(@RequestParam(required = false) Long stationId) {
        return stationId != null
                ? inventoryItemRepository.findByStation_Id(stationId)
                : inventoryItemRepository.findAll();
    }

    @GetMapping("/{id}")
    public InventoryItem getById(@PathVariable Long id) {
        return inventoryItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found: " + id));
    }

    @PostMapping
    public ResponseEntity<InventoryItem> create(@RequestParam Long stationId,
                                                 @Valid @RequestBody InventoryItem item) {
        InventoryItem saved = inventoryService.create(item, stationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public InventoryItem update(@PathVariable Long id, @Valid @RequestBody InventoryItem item) {
        return inventoryService.update(id, item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        inventoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
