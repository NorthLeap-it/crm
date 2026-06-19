package it.northleap.backend.entities;

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

// relazione tipizzata generica fra due Record, relationKey libera (es. "contact.company")
@Entity
@Table(name = "record_link",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source_id", "target_id", "relation_key"}),
        indexes = @Index(name = "idx_record_link_target_relation", columnList = "target_id, relation_key"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecordLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Record source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private Record target;

    @Column(name = "relation_key", nullable = false)
    private String relationKey;

    // metadati della relazione stessa (es. quantità in quote<->product), nullable
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecordLink other)) return false;
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
