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
import java.util.Map;
import java.util.UUID;

// Entity generica del motore metadata-driven: i valori dei campi dinamici vivono in `data` (jsonb),
// la forma è descritta da FieldDef, non dallo schema SQL. Nome "Record" voluto (stesso nome del
// modello Prisma originale), non confligge con java.lang.Record essendo in package diverso.
@Entity
@Table(name = "record", indexes = {
        @Index(name = "idx_record_object_type_status", columnList = "object_type_id, status"),
        @Index(name = "idx_record_object_type_owner", columnList = "object_type_id, owner_id"),
        @Index(name = "idx_record_title", columnList = "title")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_type_id", nullable = false)
    private ObjectType objectType;

    @Column(nullable = false)
    private String title = "";

    private String status;

    @Column(name = "owner_id")
    private UUID ownerId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> data;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Record other)) return false;
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
