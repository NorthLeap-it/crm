package it.northleap.backend.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

// "attività" è la chiave JSON dell'API originale (frontend-facing), non rinominabile a un
// identificatore Java valido senza @JsonProperty
public record ActivityPoint(
        String month,
        @JsonProperty("attività") long attivita,
        long completate
) {
}
