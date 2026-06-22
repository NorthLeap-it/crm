package it.northleap.backend.controllers;

import it.northleap.backend.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// qualunque utente auth riesce a vedere le analytics

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/{metric}")
    public ResponseEntity<List<?>> metric(@PathVariable String metric) {
        List<?> result = switch (metric) {
            case "revenue" -> analyticsService.revenue();
            case "efficiency" -> analyticsService.efficiency();
            case "pipeline" -> analyticsService.pipeline();
            case "activity" -> analyticsService.activity();
            default -> List.of();
        };
        return ResponseEntity.ok(result);
    }
}
