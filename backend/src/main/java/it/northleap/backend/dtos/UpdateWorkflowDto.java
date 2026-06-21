package it.northleap.backend.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class UpdateWorkflowDto {
    private String name;
    private String description;
    private Map<String, Object> trigger;
    private Map<String, Object> conditions;
    private List<Map<String, Object>> actions;
    private Map<String, Object> graph;
    private Boolean isActive;
}
