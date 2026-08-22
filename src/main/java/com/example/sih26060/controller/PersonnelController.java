package com.example.sih26060.controller;

import com.example.sih26060.entity.Personnel;
import com.example.sih26060.repository.PersonnelRepository;
import com.example.sih26060.security.AuthorizationSupport;
import com.example.sih26060.security.UserPrincipal;
import com.example.sih26060.service.PersonnelService;
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
@RequestMapping("/api/personnel")
@RequiredArgsConstructor
@Tag(name = "Personnel")
public class PersonnelController {

    private final PersonnelRepository personnelRepository;
    private final PersonnelService personnelService;
    private final AuthorizationSupport authorizationSupport;

    @GetMapping
    public List<Personnel> getAll(@RequestParam(required = false) Long stationId,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        Long scopedId = authorizationSupport.resolveStationId(principal, stationId);
        return scopedId != null
                ? personnelRepository.findByStation_Id(scopedId)
                : personnelRepository.findAll();
    }

    @GetMapping("/{id}")
    public Personnel getById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        Personnel person = personnelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Personnel not found: " + id));
        authorizationSupport.resolveStationId(principal, person.getStationId());
        return person;
    }

    @PostMapping
    public ResponseEntity<Personnel> create(@RequestParam Long stationId,
                                             @Valid @RequestBody Personnel person,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        Long scopedId = authorizationSupport.resolveStationId(principal, stationId);
        Personnel saved = personnelService.create(person, scopedId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Personnel update(@PathVariable Long id, @Valid @RequestBody Personnel person,
                             @AuthenticationPrincipal UserPrincipal principal) {
        Personnel existing = personnelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Personnel not found: " + id));
        authorizationSupport.resolveStationId(principal, existing.getStationId());
        return personnelService.update(id, person);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        Personnel existing = personnelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Personnel not found: " + id));
        authorizationSupport.resolveStationId(principal, existing.getStationId());
        personnelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
