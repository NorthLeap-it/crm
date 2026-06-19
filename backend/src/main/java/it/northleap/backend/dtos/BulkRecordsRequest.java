package it.northleap.backend.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BulkRecordsRequest {
    @NotEmpty
    private List<UUID> ids;

    @NotNull
    private BulkAction action;

    private Map<String, Object> set;
}
