package it.northleap.backend.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateApiKeyDto {
    @NotNull
    private String name;

    private String roleKey;
}
