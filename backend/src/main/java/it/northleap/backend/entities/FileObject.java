package it.northleap.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

// Porting di FileObject (files.module.ts, Fase 6, 04-RESTO-MODULI.md). uploadedBy/recordId
// sono UUID semplici (non relazioni JPA), stesso pattern già usato per Record.ownerId e
// Notification.userId - FK "soft", mai navigate.
@Entity
@Table(name = "file_object")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileObject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String mime;

    @Column(nullable = false)
    private long size;

    // nome randomizzato su disco, non il path completo
    @Column(nullable = false)
    private String path;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "record_id")
    private UUID recordId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileObject other)) return false;
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
