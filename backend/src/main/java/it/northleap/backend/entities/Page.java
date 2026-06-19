package it.northleap.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Pulled forward minimale in Fase 3 (solo per PageGeneratorService); CRUD completo + relazione
// charts arrivano in Fase 4 (04-RESTO-MODULI.md).
@Entity
@Table(name = "page")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_type_id")
    private ObjectType objectType;

    @Column(nullable = false, unique = true)
    private String key;

    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PageType type;

    // colonne per le liste, sezioni per i dettagli: struttura libera
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> layout;

    @Column(nullable = false)
    private boolean isSystem = false;

    @Column(nullable = false)
    private boolean isGenerated = false;

    @OneToMany(mappedBy = "page", fetch = FetchType.LAZY)
    private List<Chart> charts = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Page other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
