package it.northleap.backend.services;

import it.northleap.backend.dtos.ChartDataPoint;
import it.northleap.backend.dtos.ChartRunResponse;
import it.northleap.backend.dtos.ChartSummary;
import it.northleap.backend.dtos.CreateChartDto;
import it.northleap.backend.entities.Chart;
import it.northleap.backend.entities.ObjectType;
import it.northleap.backend.entities.Page;
import it.northleap.backend.entities.Record;
import it.northleap.backend.exceptions.BadRequestException;
import it.northleap.backend.repositories.ChartRepository;
import it.northleap.backend.repositories.ObjectTypeRepository;
import it.northleap.backend.repositories.PageRepository;
import it.northleap.backend.repositories.RecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Porting di ChartsService (charts.module.ts), incluso run(): esegue la query dichiarativa di
// un grafico sui Record di un ObjectType e ritorna i dati aggregati. `query.filters` (presente
// nel tipo TS originale) non è applicato neanche nell'originale (campo morto) — non portato qui.
@Service
@RequiredArgsConstructor
public class ChartService {

    private final ChartRepository chartRepository;
    private final ObjectTypeRepository objectTypeRepository;
    private final RecordRepository recordRepository;
    private final PageRepository pageRepository;

    public List<Chart> list() {
        return chartRepository.findAllByOrderByLabelAsc();
    }

    @Transactional
    public Chart create(CreateChartDto dto) {
        Chart chart = new Chart();
        chart.setLabel(dto.getLabel());
        chart.setType(dto.getType());
        if (dto.getPageId() != null) {
            Page page = pageRepository.findById(dto.getPageId())
                    .orElseThrow(() -> new BadRequestException("Pagina inesistente"));
            chart.setPage(page);
        }
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("objectKey", dto.getQuery().getObjectKey());
        query.put("groupBy", dto.getQuery().getGroupBy());
        query.put("aggregate", dto.getQuery().getAggregate());
        query.put("field", dto.getQuery().getField());
        chart.setQuery(query);
        chartRepository.save(chart);
        return chart;
    }

    public void remove(UUID id) {
        chartRepository.deleteById(id);
    }

    public ChartRunResponse run(UUID id) {
        Chart chart = chartRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Grafico inesistente"));
        Map<String, Object> query = chart.getQuery();
        String objectKey = query != null ? (String) query.get("objectKey") : null;
        ObjectType obj = objectKey != null ? objectTypeRepository.findByKey(objectKey).orElse(null) : null;
        if (obj == null) {
            throw new BadRequestException("Object type inesistente");
        }
        String groupBy = query.get("groupBy") != null ? String.valueOf(query.get("groupBy")) : null;
        String aggregate = query.get("aggregate") != null ? String.valueOf(query.get("aggregate")) : null;
        String field = query.get("field") != null ? String.valueOf(query.get("field")) : null;

        List<Record> records = recordRepository.findByObjectType_IdAndIsDeletedFalse(obj.getId());

        Map<String, Double> buckets = new LinkedHashMap<>();
        boolean sum = "sum".equalsIgnoreCase(aggregate) && field != null;
        for (Record r : records) {
            String key = "status".equals(groupBy)
                    ? (r.getStatus() != null ? r.getStatus() : "—")
                    : String.valueOf(r.getData() != null ? r.getData().getOrDefault(groupBy, "—") : "—");
            double inc = sum ? numericValue(r.getData() != null ? r.getData().get(field) : null) : 1;
            buckets.merge(key, inc, Double::sum);
        }

        // if/else statement (non un operatore ?:) per evitare la promozione numerica binaria di
        // Java: un'espressione condizionale con rami Double/Long unificherebbe comunque
        // entrambi a double a livello di tipo dell'espressione stessa, anche se il risultato
        // viene poi assegnato a una variabile Object — vanificando il cast a long.
        List<ChartDataPoint> data = buckets.entrySet().stream()
                .map(e -> buildDataPoint(e.getKey(), e.getValue(), sum))
                .toList();

        return new ChartRunResponse(new ChartSummary(chart.getId(), chart.getLabel(), chart.getType()), data);
    }

    private ChartDataPoint buildDataPoint(String key, double bucketValue, boolean sum) {
        if (sum) {
            return new ChartDataPoint(key, bucketValue);
        }
        return new ChartDataPoint(key, (long) bucketValue);
    }

    private double numericValue(Object raw) {
        if (raw instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }
}
