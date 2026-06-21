package it.northleap.backend.services;

import it.northleap.backend.entities.Record;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Porting puro, senza stato, di condition.ts: valuta un albero di condizioni AND/OR su un Record
// (usato sia dal trigger top-level di WorkflowEngine sia dai nodi condition/branch di
// GraphWorkflowRunner) e interpola template stringa {{record.x}}.
@Service
public class ConditionEvaluator {

    private static final Pattern TEMPLATE = Pattern.compile("\\{\\{record\\.([\\w.]+)}}");

    @SuppressWarnings("unchecked")
    public boolean evaluate(Map<String, Object> conditions, Record record) {
        if (conditions == null) {
            return true;
        }
        Object all = conditions.get("all");
        if (all instanceof List<?> list) {
            return list.stream().allMatch(rule -> evalRule((Map<String, Object>) rule, record));
        }
        Object any = conditions.get("any");
        if (any instanceof List<?> list) {
            return list.stream().anyMatch(rule -> evalRule((Map<String, Object>) rule, record));
        }
        return evalRule(conditions, record);
    }

    public String interpolate(String template, Record record) {
        if (template == null) {
            return "";
        }
        Matcher m = TEMPLATE.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String path = m.group(1);
            Object value = resolvePath(path, record);
            m.appendReplacement(out, Matcher.quoteReplacement(value != null ? String.valueOf(value) : ""));
        }
        m.appendTail(out);
        return out.toString();
    }

    private boolean evalRule(Map<String, Object> rule, Record record) {
        if (rule == null) {
            return false;
        }
        Object v = getField(String.valueOf(rule.get("field")), record);
        Object ruleValue = rule.get("value");
        return switch (String.valueOf(rule.get("op"))) {
            // eq/neq: confronto diretto come l'originale (v === rule.value), non una coercizione
            // di tipo permissiva — un numero 5 e la stringa "5" NON sono uguali, fedele a JS strict equality
            case "eq" -> java.util.Objects.equals(v, ruleValue);
            case "neq" -> !java.util.Objects.equals(v, ruleValue);
            case "gt" -> toDouble(v) > toDouble(ruleValue);
            case "lt" -> "today".equals(ruleValue)
                    ? parseDate(v) != null && parseDate(v).isBefore(Instant.now())
                    : toDouble(v) < toDouble(ruleValue);
            case "contains" -> String.valueOf(v == null ? "" : v).contains(String.valueOf(ruleValue));
            case "is_set" -> v != null && !"".equals(v);
            case "in_days" -> {
                Instant parsed = parseDate(v);
                if (v == null || parsed == null) {
                    yield false;
                }
                double diffDays = (parsed.toEpochMilli() - Instant.now().toEpochMilli()) / 86_400_000.0;
                yield diffDays >= 0 && diffDays <= toDouble(ruleValue);
            }
            default -> false;
        };
    }

    private Object getField(String field, Record record) {
        if (record == null) {
            return null;
        }
        if ("status".equals(field)) {
            return record.getStatus();
        }
        return record.getData() != null ? record.getData().get(field) : null;
    }

    @SuppressWarnings("unchecked")
    private Object resolvePath(String path, Record record) {
        if ("status".equals(path)) {
            return record != null ? record.getStatus() : null;
        }
        String[] parts = path.split("\\.");
        Object cur = record != null ? record.getData() : null;
        for (String p : parts) {
            if (!(cur instanceof Map<?, ?> m)) {
                return null;
            }
            cur = ((Map<String, Object>) m).get(p);
        }
        return cur;
    }

    private double toDouble(Object raw) {
        if (raw instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException | NullPointerException e) {
            return Double.NaN;
        }
    }

    // Parsing data permissivo: i valori dentro Record.data sono normalmente già ISO-8601 (RecordValidator
    // li normalizza al salvataggio), ma accetta anche una data semplice senza orario.
    private Instant parseDate(Object raw) {
        if (raw == null) {
            return null;
        }
        String s = String.valueOf(raw);
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException ignored) {
            // prova il formato successivo
        }
        try {
            return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
