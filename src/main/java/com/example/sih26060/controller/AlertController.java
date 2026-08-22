package com.example.sih26060.controller;

import com.example.sih26060.dto.Alert;
import com.example.sih26060.security.AuthorizationSupport;
import com.example.sih26060.security.UserPrincipal;
import com.example.sih26060.service.AlertService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts")
public class AlertController {

    private final AlertService alertService;
    private final AuthorizationSupport authorizationSupport;

    @GetMapping
    public List<Alert> getRecent(@AuthenticationPrincipal UserPrincipal principal) {
        Long scopedId = authorizationSupport.resolveStationId(principal, null);
        List<Alert> recent = alertService.getRecent();
        return scopedId != null
                ? recent.stream().filter(a -> scopedId.equals(a.stationId())).toList()
                : recent;
    }
}
