package it.northleap.backend.controllers;

import it.northleap.backend.dtos.ChartRunResponse;
import it.northleap.backend.dtos.CreateChartDto;
import it.northleap.backend.entities.Chart;
import it.northleap.backend.security.PermAction;
import it.northleap.backend.security.RequirePerm;
import it.northleap.backend.services.ChartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Porting di ChartsController (charts.module.ts).
@RestController
@RequestMapping("/api/charts")
@RequiredArgsConstructor
public class ChartController {

    private final ChartService chartService;

    @GetMapping
    @RequirePerm(resource = "chart", action = PermAction.READ)
    public ResponseEntity<List<Chart>> list() {
        return ResponseEntity.ok(chartService.list());
    }

    @GetMapping("/{id}/run")
    @RequirePerm(resource = "chart", action = PermAction.READ)
    public ResponseEntity<ChartRunResponse> run(@PathVariable UUID id) {
        return ResponseEntity.ok(chartService.run(id));
    }

    @PostMapping
    @RequirePerm(resource = "chart", action = PermAction.WRITE)
    public ResponseEntity<Chart> create(@Valid @RequestBody CreateChartDto dto) {
        return ResponseEntity.ok(chartService.create(dto));
    }

    @DeleteMapping("/{id}")
    @RequirePerm(resource = "chart", action = PermAction.WRITE)
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        chartService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
