package it.northleap.backend.services;

import it.northleap.backend.dtos.ActivityPoint;
import it.northleap.backend.dtos.PipelinePoint;
import it.northleap.backend.dtos.RevenuePoint;
import it.northleap.backend.entities.ObjectType;
import it.northleap.backend.entities.Record;
import it.northleap.backend.repositories.ObjectTypeRepository;
import it.northleap.backend.repositories.RecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ObjectTypeRepository objectTypeRepository;
    @Mock
    private RecordRepository recordRepository;

    private AnalyticsService analyticsService;
    private final LocalDate referenceDate = LocalDate.of(2026, 6, 15);

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(objectTypeRepository, recordRepository);
        lenient().when(objectTypeRepository.findByKey(any())).thenReturn(Optional.empty());
    }

    private ObjectType objectType(String key) {
        ObjectType obj = new ObjectType();
        obj.setId(UUID.randomUUID());
        obj.setKey(key);
        return obj;
    }

    private Record record(String status, Map<String, Object> data, Instant createdAt) {
        Record r = new Record();
        r.setStatus(status);
        r.setData(data);
        r.setCreatedAt(createdAt);
        return r;
    }

    private void stub(String key, List<Record> records) {
        ObjectType obj = objectType(key);
        when(objectTypeRepository.findByKey(key)).thenReturn(Optional.of(obj));
        when(recordRepository.findByObjectType_IdAndIsDeletedFalse(obj.getId())).thenReturn(records);
    }

    @Test
    void revenueSumsAmountsInTheCorrectMonthBucket() {
        Instant currentMonth = java.time.YearMonth.from(referenceDate).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant lastMonth = referenceDate.minusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        stub("invoice", List.of(
                record(null, Map.of("amount", 1000), currentMonth),
                record(null, Map.of("amount", 500), currentMonth),
                record(null, Map.of("amount", 2000), lastMonth)
        ));

        List<RevenuePoint> points = analyticsService.revenue(referenceDate);

        assertEquals(6, points.size());
        RevenuePoint current = points.get(points.size() - 1);
        assertEquals(1500, current.fatturato());
        assertEquals(Math.round(1500 * 0.55), current.costi());

        RevenuePoint previous = points.get(points.size() - 2);
        assertEquals(2000, previous.fatturato());
    }

    @Test
    void pipelineGroupsOpportunitiesByStatus() {
        stub("opportunity", List.of(
                record("won", Map.of(), Instant.now()),
                record("won", Map.of(), Instant.now()),
                record("lost", Map.of(), Instant.now()),
                record(null, Map.of(), Instant.now())
        ));

        List<PipelinePoint> points = analyticsService.pipeline();

        Map<String, Long> byName = points.stream()
                .collect(java.util.stream.Collectors.toMap(PipelinePoint::name, PipelinePoint::value));
        assertEquals(2L, byName.get("won"));
        assertEquals(1L, byName.get("lost"));
        assertEquals(1L, byName.get("n/d"));
    }

    @Test
    void activityCountsTotalAndDoneInCurrentMonth() {
        Instant currentMonth = java.time.YearMonth.from(referenceDate).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        stub("task", List.of(
                record("done", Map.of(), currentMonth),
                record("todo", Map.of(), currentMonth),
                record("doing", Map.of(), currentMonth)
        ));

        List<ActivityPoint> points = analyticsService.activity(referenceDate);

        ActivityPoint current = points.get(points.size() - 1);
        assertEquals(3, current.attivita());
        assertEquals(1, current.completate());
    }
}
