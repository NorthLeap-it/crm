package it.northleap.backend.services;

import it.northleap.backend.entities.AuditLog;
import it.northleap.backend.repositories.AuditLogRepository;
import it.northleap.backend.security.Actor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

// Versione minima, pulled forward dalla Fase 6 (04-RESTO-MODULI.md): solo scrittura, usata da
// RecordsService. Nessun endpoint di lettura (GET /logs) in questa fase.
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(Actor actor, String action, String resource, String resourceId, Map<String, Object> diff, String ip) {
        AuditLog entry = new AuditLog();
        entry.setActorId(actor != null ? actor.id() : null);
        entry.setActorType(actor != null ? actor.type().name().toLowerCase() : "system");
        entry.setAction(action);
        entry.setResource(resource);
        entry.setResourceId(resourceId);
        entry.setDiff(diff);
        entry.setIp(ip);
        auditLogRepository.save(entry);
    }
}
