package it.northleap.backend.events;

import it.northleap.backend.entities.Notification;

import java.util.UUID;

// equivalente di events.emit('notify', ...) nell'originale (per il realtime via WebSocket);
// nessun listener ancora — il realtime resta esplicitamente fuori scope (04-RESTO-MODULI.md),
// stesso pattern "nessun listener" di RecordCreatedEvent in Fase 3.
public record NotifyEvent(UUID userId, Notification notification) {
}
