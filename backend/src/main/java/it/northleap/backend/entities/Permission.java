package it.northleap.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

// resource è una stringa libera (key di un ObjectType, oppure risorsa di sistema fissa come
// "page"/"chart"/"workflow"/"user"/"apikey") e non una FK a ObjectType: deve poter referenziare
// anche risorse che non sono ObjectType
@Entity
@Table(name = "permission", uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "resource"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(nullable = false)
    private String resource;

    @Column(nullable = false)
    private boolean canRead = false;

    @Column(nullable = false)
    private boolean canWrite = false;

    @Column(nullable = false)
    private boolean canExecute = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermScope scope = PermScope.OWN;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission other)) return false;
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
