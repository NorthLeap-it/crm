package it.northleap.backend.services;

import it.northleap.backend.dtos.ChartDataPoint;
import it.northleap.backend.dtos.ChartRunResponse;
import it.northleap.backend.entities.Chart;
import it.northleap.backend.entities.ObjectType;
import it.northleap.backend.entities.Record;
import it.northleap.backend.exceptions.BadRequestException;
import it.northleap.backend.repositories.ChartRepository;
import it.northleap.backend.repositories.ObjectTypeRepository;
import it.northleap.backend.repositories.PageRepository;
import it.northleap.backend.repositories.RecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChartServiceTest {

    @Mock
    private ChartRepository chartRepository;
    @Mock
    private ObjectTypeRepository objectTypeRepository;
    @Mock
    private RecordRepository recordRepository;
    @Mock
    private PageRepository pageRepository;

    private ChartService chartService;
    private ObjectType obj;

    @BeforeEach
    void setUp() {
        chartService = new ChartService(chartRepository, objectTypeRepository, recordRepository, pageRepository);
        obj = new ObjectType();
        obj.setId(UUID.randomUUID());
        obj.setKey("opportunity");
    }

    private Chart chart(Map<String, Object> query) {
        Chart c = new Chart();
        c.setId(UUID.randomUUID());
        c.setLabel("Pipeline");
        c.setQuery(query);
        return c;
    }

    private Record record(String status, Map<String, Object> data) {
        Record r = new Record();
        r.setStatus(status);
        r.setData(data);
        return r;
    }

    @Test
    void groupsByStatusWithDefaultCountAggregate() {
        Chart chart = chart(Map.of("objectKey", "opportunity", "groupBy", "status"));
        when(chartRepository.findById(chart.getId())).thenReturn(java.util.Optional.of(chart));
        when(objectTypeRepository.findByKey("opportunity")).thenReturn(java.util.Optional.of(obj));
        when(recordRepository.findByObjectType_IdAndIsDeletedFalse(obj.getId())).thenReturn(List.of(
                record("won", Map.of()),
                record("won", Map.of()),
                record("lost", Map.of())
        ));

        ChartRunResponse response = chartService.run(chart.getId());

        assertEquals(2, response.data().size());
        Map<String, Object> byLabel = response.data().stream()
                .collect(java.util.stream.Collectors.toMap(ChartDataPoint::label, ChartDataPoint::value));
        assertEquals(2L, byLabel.get("won"));
        assertEquals(1L, byLabel.get("lost"));
    }

    @Test
    void groupsByDynamicDataField() {
        Chart chart = chart(Map.of("objectKey", "opportunity", "groupBy", "stage"));
        when(chartRepository.findById(chart.getId())).thenReturn(java.util.Optional.of(chart));
        when(objectTypeRepository.findByKey("opportunity")).thenReturn(java.util.Optional.of(obj));
        when(recordRepository.findByObjectType_IdAndIsDeletedFalse(obj.getId())).thenReturn(List.of(
                record(null, Map.of("stage", "prospect")),
                record(null, Map.of("stage", "won"))
        ));

        ChartRunResponse response = chartService.run(chart.getId());

        assertEquals(2, response.data().size());
    }

    @Test
    void sumAggregatesNumericDataField() {
        Chart chart = chart(Map.of("objectKey", "opportunity", "groupBy", "status", "aggregate", "sum", "field", "amount"));
        when(chartRepository.findById(chart.getId())).thenReturn(java.util.Optional.of(chart));
        when(objectTypeRepository.findByKey("opportunity")).thenReturn(java.util.Optional.of(obj));
        when(recordRepository.findByObjectType_IdAndIsDeletedFalse(obj.getId())).thenReturn(List.of(
                record("won", Map.of("amount", 1000)),
                record("won", Map.of("amount", 500))
        ));

        ChartRunResponse response = chartService.run(chart.getId());

        assertEquals(1, response.data().size());
        assertEquals(1500.0, response.data().get(0).value());
    }

    @Test
    void unknownObjectKeyThrowsBadRequest() {
        Chart chart = chart(Map.of("objectKey", "doesnotexist"));
        when(chartRepository.findById(chart.getId())).thenReturn(java.util.Optional.of(chart));
        when(objectTypeRepository.findByKey("doesnotexist")).thenReturn(java.util.Optional.empty());

        assertThrows(BadRequestException.class, () -> chartService.run(chart.getId()));
    }

    @Test
    void unknownChartThrowsBadRequest() {
        UUID id = UUID.randomUUID();
        when(chartRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThrows(BadRequestException.class, () -> chartService.run(id));
    }
}
