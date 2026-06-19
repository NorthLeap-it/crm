package it.northleap.backend.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SortSpec {
    private String field;
    private String dir; // "asc" | "desc"
}
