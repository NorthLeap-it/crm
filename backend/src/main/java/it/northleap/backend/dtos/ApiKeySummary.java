package it.northleap.backend.dtos;

import java.time.Instant;
import java.util.UUID;

// proiezione di lista: niente keyHash, mai la chiave intera (solo il prefix per riconoscerla)
public record ApiKeySummary(UUID id, String name, String prefix, Instant lastUsedAt, Instant createdAt) {
}
