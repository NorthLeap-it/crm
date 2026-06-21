package it.northleap.backend.repositories;

import it.northleap.backend.entities.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findTop100ByResourceAndResourceIdOrderByCreatedAtDesc(String resource, String resourceId);
    List<AuditLog> findTop100ByResourceOrderByCreatedAtDesc(String resource);
    List<AuditLog> findTop100ByResourceIdOrderByCreatedAtDesc(String resourceId);
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}
