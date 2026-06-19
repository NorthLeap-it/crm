package it.northleap.backend.dtos;

import java.util.List;

public record ChartRunResponse(ChartSummary chart, List<ChartDataPoint> data) {
}
