package com.example.sih26060.controller;

import com.example.sih26060.entity.Equipment;
import com.example.sih26060.repository.EquipmentRepository;
import com.example.sih26060.service.EquipmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Tag(name = "Equipment")
public class EquipmentController {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentService equipmentService;

    @GetMapping
    public List<Equipment> getAll(@RequestParam(required = false) Long stationId) {
        return stationId != null
                ? equipmentRepository.findByStation_Id(stationId)
                : equipmentRepository.findAll();
    }

    @GetMapping("/{id}")
    public Equipment getById(@PathVariable Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Equipment not found: " + id));
    }

    @PostMapping
    public ResponseEntity<Equipment> create(@RequestParam Long stationId,
                                             @Valid @RequestBody Equipment equipment) {
        Equipment saved = equipmentService.create(equipment, stationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Equipment update(@PathVariable Long id, @Valid @RequestBody Equipment equipment) {
        return equipmentService.update(id, equipment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        equipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
