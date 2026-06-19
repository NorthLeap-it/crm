package it.northleap.backend.events;

import it.northleap.backend.entities.Record;

import java.util.Map;

public record RecordUpdatedEvent(String objectKey, Record record, Map<String, Object> before) {
}
