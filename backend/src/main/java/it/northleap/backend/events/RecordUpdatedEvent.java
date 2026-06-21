package it.northleap.backend.events;

import it.northleap.backend.entities.Record;

import java.util.Map;

// beforeStatus: necessario per il rilevamento "field.changed" del workflow engine (Fase 5),
// che deve scattare se cambia il campo dati indicato OPPURE lo status — status vive in una
// colonna separata da `data`, quindi non basta confrontare `before` (solo i dati pre-modifica).
public record RecordUpdatedEvent(String objectKey, Record record, Map<String, Object> before, String beforeStatus) {
}
