package it.northleap.backend.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class CreateWorkflowDto {
    @NotNull
    private String name;

    private String description;

    @NotNull
    private Map<String, Object> trigger;

    private Map<String, Object> conditions;

    private List<Map<String, Object>> actions;

    private Map<String, Object> graph;

    private Boolean isActive;
}
