package it.northleap.backend.events;

import it.northleap.backend.entities.Record;

public record RecordDeletedEvent(String objectKey, Record record) {
}
