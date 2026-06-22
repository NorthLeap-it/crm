package it.northleap.backend.services;

import it.northleap.backend.dtos.ActivityPoint;
import it.northleap.backend.dtos.EfficiencyPoint;
import it.northleap.backend.dtos.PipelinePoint;
import it.northleap.backend.dtos.RevenuePoint;
import it.northleap.backend.entities.ObjectType;
import it.northleap.backend.entities.Record;
import it.northleap.backend.repositories.ObjectTypeRepository;
import it.northleap.backend.repositories.RecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Porting di AnalyticsService (analytics.module.ts): nessuna entity propria, legge i Record
// esistenti e calcola serie temporali per la dashboard. lastMonths()/i metodi pubblici hanno un
// overload package-private con LocalDate esplicito per essere testabili senza mockare l'orologio
// di sistema — il pubblico resta invariato (usa LocalDate.now()).
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final String[] MONTHS = {"Gen", "Feb", "Mar", "Apr", "Mag", "Giu", "Lug", "Ago", "Set", "Ott", "Nov", "Dic"};
    private static final Set<String> DONE_STATUSES = Set.of("done", "closed", "completed", "completato", "chiuso");
    private static final Set<String> ACTIVITY_DONE_STATUSES = Set.of("done", "completed", "completato");

    private final ObjectTypeRepository objectTypeRepository;
    private final RecordRepository recordRepository;

    private record MonthBucket(String label, Instant start, Instant end) {
    }

    // metodo che recupera gli ultimi n mesi
    List<MonthBucket> lastMonths(int n, LocalDate referenceDate) {
        List<MonthBucket> out = new java.util.ArrayList<>();
        YearMonth current = YearMonth.from(referenceDate);
        for (int i = n - 1; i >= 0; i--) {
            YearMonth m = current.minusMonths(i);
            Instant start = m.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = m.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            out.add(new MonthBucket(MONTHS[m.getMonthValue() - 1], start, end));
        }
        return out;
    }

    private List<Record> records(String objectKey) {
        return objectTypeRepository.findByKey(objectKey)
                .map(ObjectType::getId)
                .map(recordRepository::findByObjectType_IdAndIsDeletedFalse)
                .orElse(List.of());
    }

    // primo valore non-NaN e non-zero tra le chiavi candidate, altrimenti 0 (stessa semantica
    // "truthy" dell'originale: anche uno 0 esplicito viene saltato a favore della chiave successiva)
    private double numField(Map<String, Object> data, List<String> keys) {
        if (data == null) {
            return 0;
        }
        for (String k : keys) {
            Object raw = data.get(k);
            if (raw == null) {
                continue;
            }
            double v;
            try {
                v = (raw instanceof Number n) ? n.doubleValue() : Double.parseDouble(String.valueOf(raw));
            } catch (NumberFormatException e) {
                continue;
            }
            if (v != 0) {
                return v;
            }
        }
        return 0;
    }

    private boolean inRange(Record r, MonthBucket m) {
        Instant createdAt = r.getCreatedAt();
        return createdAt != null && !createdAt.isBefore(m.start()) && createdAt.isBefore(m.end());
    }

    public List<RevenuePoint> revenue() {
        return revenue(LocalDate.now());
    }

    List<RevenuePoint> revenue(LocalDate referenceDate) {
        // legge tutti i record fatture
        List<Record> invoices = records("invoice");
        return lastMonths(6, referenceDate).stream()
                .map(m -> {
                    double fatturato = invoices.stream()
                            .filter(r -> inRange(r, m))
                            .mapToDouble(r -> numField(r.getData(), List.of("amount", "total", "importo", "totale")))
                            .sum();
                    return new RevenuePoint(m.label(), Math.round(fatturato), Math.round(fatturato * 0.55));
                })
                .toList();
    }

    public List<EfficiencyPoint> efficiency() {
        return efficiency(LocalDate.now());
    }

    List<EfficiencyPoint> efficiency(LocalDate referenceDate) {
        // analizza sia task che ticket
        List<Record> all = new java.util.ArrayList<>(records("task"));
        all.addAll(records("ticket"));
        return lastMonths(6, referenceDate).stream()
                .map(m -> {
                    List<Record> inMonth = all.stream().filter(r -> inRange(r, m)).toList();
                    long done = inMonth.stream()
                            .filter(r -> DONE_STATUSES.contains(statusOf(r)))
                            .count();
                    int efficienza = inMonth.isEmpty() ? 0 : (int) Math.round((done * 100.0) / inMonth.size());
                    return new EfficiencyPoint(m.label(), efficienza);
                })
                .toList();
    }

    public List<PipelinePoint> pipeline() {
        // opportyunity attive
        List<Record> opps = records("opportunity");
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Record r : opps) {
            String status = r.getStatus() != null ? r.getStatus() : "n/d";
            byStatus.merge(status, 1L, Long::sum);
        }
        return byStatus.entrySet().stream().map(e -> new PipelinePoint(e.getKey(), e.getValue())).toList();
    }

    /**
     * metodo che ritorna i task presenti negli ultimi 6 mesi
     * con i vari status (completato, in corso)
     * @return
     */
    public List<ActivityPoint> activity() {
        return activity(LocalDate.now());
    }

    // metodo helper
    List<ActivityPoint> activity(LocalDate referenceDate) {
        // record di task
        List<Record> tasks = records("task");
        // statistiche degli ultimi 6 mesi
        return lastMonths(6, referenceDate).stream()
                .map(m -> {
                    List<Record> inMonth = tasks.stream().filter(r -> inRange(r, m)).toList();
                    long completate = inMonth.stream()
                            .filter(r -> ACTIVITY_DONE_STATUSES.contains(statusOf(r)))
                            .count();
                    return new ActivityPoint(m.label(), inMonth.size(), completate);
                })
                .toList();
    }

    private String statusOf(Record r) {
        return r.getStatus() != null ? r.getStatus().toLowerCase() : "";
    }
}
