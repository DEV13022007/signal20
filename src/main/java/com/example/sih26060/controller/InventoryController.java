package com.example.sih26060.controller;

import com.example.sih26060.entity.InventoryItem;
import com.example.sih26060.repository.InventoryItemRepository;
import com.example.sih26060.security.AuthorizationSupport;
import com.example.sih26060.security.UserPrincipal;
import com.example.sih26060.service.InventoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory")
public class InventoryController {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryService inventoryService;
    private final AuthorizationSupport authorizationSupport;

    @GetMapping
    public List<InventoryItem> getAll(@RequestParam(required = false) Long stationId,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        Long scopedId = authorizationSupport.resolveStationId(principal, stationId);
        return scopedId != null
                ? inventoryItemRepository.findByStation_Id(scopedId)
                : inventoryItemRepository.findAll();
    }

    @GetMapping("/{id}")
    public InventoryItem getById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found: " + id));
        authorizationSupport.resolveStationId(principal, item.getStationId());
        return item;
    }

    @PostMapping
    public ResponseEntity<InventoryItem> create(@RequestParam Long stationId,
                                                 @Valid @RequestBody InventoryItem item,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        Long scopedId = authorizationSupport.resolveStationId(principal, stationId);
        InventoryItem saved = inventoryService.create(item, scopedId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public InventoryItem update(@PathVariable Long id, @Valid @RequestBody InventoryItem item,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        InventoryItem existing = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found: " + id));
        authorizationSupport.resolveStationId(principal, existing.getStationId());
        return inventoryService.update(id, item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        InventoryItem existing = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found: " + id));
        authorizationSupport.resolveStationId(principal, existing.getStationId());
        inventoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
