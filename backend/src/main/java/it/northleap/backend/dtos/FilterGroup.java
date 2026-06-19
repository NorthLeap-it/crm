package it.northleap.backend.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Albero di filtri AND/OR annidati, porting di FilterGroup/FilterCondition (filter-builder.ts).
// `conditions` resta List<Object> di proposito: ogni elemento è o un'altra FilterGroup-shape o
// una condizione foglia ({field,op,value,value2}); Jackson li deserializza come LinkedHashMap
// finché il target dichiarato è Object, e RecordQueryService li distingue a runtime guardando
// la presenza della chiave "combinator" — stessa logica strutturale di isGroup() nell'originale,
// niente bisogno di un deserializzatore polimorfico custom.
@Getter
@Setter
@NoArgsConstructor
public class FilterGroup {
    private String combinator; // "and" | "or"
    private List<Object> conditions;
}
