package it.northleap.backend.dtos;

import it.northleap.backend.entities.ChartType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreateChartDto {
    @NotNull
    private String label;

    @NotNull
    private ChartType type;

    private UUID pageId;

    @NotNull
    @Valid
    private ChartQueryDto query;
}
