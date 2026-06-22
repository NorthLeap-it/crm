package it.northleap.backend.dtos;

import it.northleap.backend.entities.WebhookDirection;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// proiezione di lista: niente secret (usato per firmare/verificare HMAC) - visibile solo nella
// risposta di create(), stesso trattamento gia' riservato alla chiave in chiaro di ApiKeySummary.
// Campo chiamato "active" (non "isActive"): l'entity Webhook serializza isActive() -> "active"
// nel JSON (Jackson spoglia il prefisso "is" da un getter booleano), stesso nome qui per non
// cambiare la forma della risposta vista finora dal frontend.
public record WebhookSummary(UUID id, WebhookDirection direction, String name, String url,
                              List<String> events, boolean active, Instant createdAt) {
}
