package com.example.sih26060.controller;

import com.example.sih26060.entity.Personnel;
import com.example.sih26060.repository.PersonnelRepository;
import com.example.sih26060.service.PersonnelService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personnel")
@RequiredArgsConstructor
@Tag(name = "Personnel")
public class PersonnelController {

    private final PersonnelRepository personnelRepository;
    private final PersonnelService personnelService;

    @GetMapping
    public List<Personnel> getAll(@RequestParam(required = false) Long stationId) {
        return stationId != null
                ? personnelRepository.findByStation_Id(stationId)
                : personnelRepository.findAll();
    }

    @GetMapping("/{id}")
    public Personnel getById(@PathVariable Long id) {
        return personnelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Personnel not found: " + id));
    }

    @PostMapping
    public ResponseEntity<Personnel> create(@RequestParam Long stationId,
                                             @Valid @RequestBody Personnel person) {
        Personnel saved = personnelService.create(person, stationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Personnel update(@PathVariable Long id, @Valid @RequestBody Personnel person) {
        return personnelService.update(id, person);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        personnelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
