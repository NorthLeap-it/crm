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
public class UpsertRecordDto {
    private String title;
    private String status;
    private UUID ownerId;

    @NotNull
    private Map<String, Object> data;
}
