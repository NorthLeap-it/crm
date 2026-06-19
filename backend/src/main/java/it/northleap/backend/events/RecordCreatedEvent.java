package it.northleap.backend.events;

import it.northleap.backend.entities.Record;

// equivalente di EventEmitter2.emit('record.created', ...) nell'originale; nessun listener
// ancora (il workflow engine, Fase 5, sottoscriverà questi eventi)
public record RecordCreatedEvent(String objectKey, Record record) {
}
