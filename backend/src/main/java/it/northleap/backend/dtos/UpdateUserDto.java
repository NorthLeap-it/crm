package it.northleap.backend.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserDto {
    private String name;
    private Boolean isActive;
    private List<String> roleKeys;
}
