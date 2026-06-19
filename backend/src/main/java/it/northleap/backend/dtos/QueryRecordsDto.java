package it.northleap.backend.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class QueryRecordsDto {
    private String q;
    private String status;
    private Integer page;
    private Integer pageSize;
    private FilterGroup filter;
    private List<SortSpec> sort;
}
