package it.northleap.backend.dtos;

import it.northleap.backend.entities.PageType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreatePageDto {
    @NotNull
    private String key;

    @NotNull
    private String label;

    @NotNull
    private PageType type;

    private UUID objectTypeId;

    @NotNull
    private Map<String, Object> layout;
}
