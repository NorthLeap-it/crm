package it.northleap.backend.services;

import it.northleap.backend.entities.AuditLog;
import it.northleap.backend.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// Porting di AuditQueryService (audit.module.ts). Quattro combinazioni di filtro possibili
// (resource/resourceId entrambi, uno solo, nessuno) - quattro query dedicate invece di una
// query dinamica, più semplice per così poche varianti.
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;

    public List<AuditLog> list(String resource, String resourceId) {
        if (resource != null && resourceId != null) {
            return auditLogRepository.findTop100ByResourceAndResourceIdOrderByCreatedAtDesc(resource, resourceId);
        }
        if (resource != null) {
            return auditLogRepository.findTop100ByResourceOrderByCreatedAtDesc(resource);
        }
        if (resourceId != null) {
            return auditLogRepository.findTop100ByResourceIdOrderByCreatedAtDesc(resourceId);
        }
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }
}
