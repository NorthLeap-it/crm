package it.northleap.backend.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class UpdatePageDto {
    private String label;
    private Map<String, Object> layout;
}
