package it.northleap.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

// Porting di ApiKey (api-keys.module.ts, Fase 6, 04-RESTO-MODULI.md). La chiave in chiaro
// (nl_ + 24 byte random hex) non è mai persistita: solo keyHash (SHA-256) + prefix (primi 10
// char, solo per riconoscerla nelle liste) sopravvivono alla creazione.
@Entity
@Table(name = "api_key")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String keyHash;

    @Column(nullable = false)
    private String prefix;

    // il ruolo definisce cosa la chiave può fare via RBAC, nullable (nessun permesso)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    private Instant lastUsedAt;

    private Instant expiresAt;

    private Instant revokedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApiKey other)) return false;
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
