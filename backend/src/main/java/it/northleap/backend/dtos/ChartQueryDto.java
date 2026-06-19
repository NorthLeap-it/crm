package it.northleap.backend.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// {objectKey, groupBy?, aggregate?, field?} — aggregate resta String ("count"/"sum", confrontato
// case-insensitive) invece di un enum stretto, stessa ampiezza del literal union 'count'|'sum'
// dell'originale. `filters?` dell'originale non è portato: dichiarato nel tipo TS ma mai
// applicato in run() (campo morto nell'originale) — vedi nota in CLAUDE.md.
@Getter
@Setter
@NoArgsConstructor
public class ChartQueryDto {
    @NotNull
    private String objectKey;

    private String groupBy;
    private String aggregate;
    private String field;
}
