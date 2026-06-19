package it.northleap.backend.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateObjectTypeDto {
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_]{1,64}$")
    private String key;

    @NotBlank
    private String label;

    @NotBlank
    private String pluralLabel;

    private String icon;
    private String color;

    @Valid
    private List<FieldDefDto> fields;
}
