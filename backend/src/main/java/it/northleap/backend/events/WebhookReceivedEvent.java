package it.northleap.backend.events;

import java.util.UUID;

// equivalente di events.emit('webhook.inbound', ...) nell'originale; nessun listener ancora,
// stesso trattamento di NotifyEvent (Fase 5) e RecordCreatedEvent (Fase 3).
public record WebhookReceivedEvent(UUID webhookId, Object payload) {
}
