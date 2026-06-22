package it.northleap.backend.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
* descrizione di come è una colonna del db
*/

@Entity
@Table(name = "field_def", uniqueConstraints = @UniqueConstraint(columnNames = {"object_type_id", "key"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldDef {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // @JsonIgnore: evita il ciclo infinito ObjectType.fields <-> FieldDef.objectType in
    // serializzazione (il client ottiene già i field dentro ObjectType.fields)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_type_id", nullable = false)
    @JsonIgnore
    private ObjectType objectType;

    @Column(nullable = false)
    private String key;

    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType type;

    private String description;

    private String placeholder;

    @Column(nullable = false)
    private boolean required = false;

    @Column(name = "is_unique", nullable = false)
    private boolean isUnique = false;

    @Column(nullable = false)
    private boolean isIndexed = false;

    @Column(nullable = false)
    private boolean isReadonly = false;

    @Column(nullable = false)
    private boolean isHidden = false;

    @Column(nullable = false)
    private boolean isFilterable = true;

    @Column(nullable = false)
    private boolean isSortable = true;

    @Column(nullable = false)
    private boolean showInList = true;

    // valore di default: string/number/bool/array, qualunque forma JSON. Object generico (non
    // JsonNode): Hibernate 7.2 mappa @JdbcTypeCode(SqlTypes.JSON) via Jackson classico
    // (com.fasterxml.jackson.databind, l'unico che conosce — vedi nota in CLAUDE.md), che è in
    // conflitto di versione con l'ObjectMapper Jackson 3 usato da Spring per le risposte HTTP.
    // Map/List/Object "nudi" funzionano con entrambi senza richiedere un tipo specifico di libreria.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object defaultValue;

    private Float min;

    private Float max;

    private Float step;

    private String pattern;

    private String section;

    @Column(nullable = false)
    private String width = "full";

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    // select/multiselect/rating/status: [{value,label,color}]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> options;

    // relation/formula/rollup/lookup: es. {targetObject, multiple}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> config;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FieldDef other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
