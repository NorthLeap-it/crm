package it.northleap.backend.dtos;

import it.northleap.backend.entities.FieldType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class FieldDefDto {
    @NotNull
    @Pattern(regexp = "^[a-zA-Z0-9_]{1,64}$")
    private String key;

    @NotNull
    private String label;

    @NotNull
    private FieldType type;

    private String description;
    private String placeholder;
    private Boolean required;
    private Boolean isUnique;
    private Boolean isIndexed;
    private Boolean isReadonly;
    private Boolean isHidden;
    private Boolean isFilterable;
    private Boolean isSortable;
    private Boolean showInList;
    private Object defaultValue;
    private Float min;
    private Float max;
    private Float step;
    private String pattern;
    private String section;
    private String width;
    private List<Map<String, Object>> options;
    private Map<String, Object> config;
    private Integer sortOrder;
}
