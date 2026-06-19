package it.northleap.backend.dtos;

import it.northleap.backend.entities.ChartType;

import java.util.UUID;

public record ChartSummary(UUID id, String label, ChartType type) {
}
