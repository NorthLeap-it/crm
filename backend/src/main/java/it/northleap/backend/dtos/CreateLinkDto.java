package it.northleap.backend.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreateLinkDto {
    @NotNull
    private UUID targetId;

    @NotNull
    private String relationKey;

    private Map<String, Object> data;
}
