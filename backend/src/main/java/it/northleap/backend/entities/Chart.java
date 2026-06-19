package it.northleap.backend.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

// Porting di charts.module.ts (Fase 4, 04-RESTO-MODULI.md).
@Entity
@Table(name = "chart")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Chart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // @JsonIgnore: evita il ciclo Page.charts <-> Chart.page, stesso motivo di
    // FieldDef.objectType in ObjectType.fields (Fase 3)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id")
    @JsonIgnore
    private Page page;

    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChartType type;

    // {objectKey, groupBy?, aggregate?, field?} — Object generico, non JsonNode (vedi nota Jackson in CLAUDE.md)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> query;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Chart other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
