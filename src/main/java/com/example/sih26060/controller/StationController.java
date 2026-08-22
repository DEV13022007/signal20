package com.example.sih26060.controller;

import com.example.sih26060.entity.Station;
import com.example.sih26060.repository.StationRepository;
import com.example.sih26060.security.AuthorizationSupport;
import com.example.sih26060.security.UserPrincipal;
import com.example.sih26060.service.ReportService;
import com.example.sih26060.service.StationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@Tag(name = "Stations")
public class StationController {

    private final StationRepository stationRepository;
    private final StationService stationService;
    private final ReportService reportService;
    private final AuthorizationSupport authorizationSupport;

    @GetMapping
    public List<Station> getAll(@AuthenticationPrincipal UserPrincipal principal) {
        Long scopedId = authorizationSupport.resolveStationId(principal, null);
        if (scopedId == null) {
            return stationRepository.findAll();
        }
        return stationRepository.findById(scopedId).map(List::of).orElseGet(List::of);
    }

    @GetMapping("/{id}")
    public Station getById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        authorizationSupport.resolveStationId(principal, id);
        return stationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Station not found: " + id));
    }

    @PostMapping
    public ResponseEntity<Station> create(@Valid @RequestBody Station station) {
        station.setId(null);
        station.setSatelliteLinkActive(false);
        Station saved = stationRepository.save(station);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{id}/satellite-link")
    public Station setSatelliteLink(@PathVariable Long id, @RequestParam boolean active,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        authorizationSupport.resolveStationId(principal, id);
        return stationService.setSatelliteLink(id, active);
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> getReport(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        authorizationSupport.resolveStationId(principal, id);
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Station not found: " + id));
        byte[] pdf = reportService.generateStationReport(id);
        String filename = "%s-status-report-%s.pdf".formatted(
                station.getCode().toLowerCase(), java.time.LocalDate.now());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(pdf);
    }
}
