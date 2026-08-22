package com.example.sih26060.controller;

import com.example.sih26060.entity.Equipment;
import com.example.sih26060.repository.EquipmentRepository;
import com.example.sih26060.security.AuthorizationSupport;
import com.example.sih26060.security.UserPrincipal;
import com.example.sih26060.service.EquipmentService;
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
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Tag(name = "Equipment")
public class EquipmentController {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentService equipmentService;
    private final AuthorizationSupport authorizationSupport;

    @GetMapping
    public List<Equipment> getAll(@RequestParam(required = false) Long stationId,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        Long scopedId = authorizationSupport.resolveStationId(principal, stationId);
        return scopedId != null
                ? equipmentRepository.findByStation_Id(scopedId)
                : equipmentRepository.findAll();
    }

    @GetMapping("/{id}")
    public Equipment getById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Equipment not found: " + id));
        authorizationSupport.resolveStationId(principal, equipment.getStationId());
        return equipment;
    }

    @PostMapping
    public ResponseEntity<Equipment> create(@RequestParam Long stationId,
                                             @Valid @RequestBody Equipment equipment,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        Long scopedId = authorizationSupport.resolveStationId(principal, stationId);
        Equipment saved = equipmentService.create(equipment, scopedId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Equipment update(@PathVariable Long id, @Valid @RequestBody Equipment equipment,
                             @AuthenticationPrincipal UserPrincipal principal) {
        Equipment existing = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Equipment not found: " + id));
        authorizationSupport.resolveStationId(principal, existing.getStationId());
        return equipmentService.update(id, equipment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        Equipment existing = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Equipment not found: " + id));
        authorizationSupport.resolveStationId(principal, existing.getStationId());
        equipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
