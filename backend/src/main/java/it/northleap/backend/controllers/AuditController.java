package it.northleap.backend.controllers;

import it.northleap.backend.entities.AuditLog;
import it.northleap.backend.security.PermAction;
import it.northleap.backend.security.RequirePerm;
import it.northleap.backend.services.AuditQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Porting di AuditController (audit.module.ts). Risorsa RBAC "user" come l'originale: chi
// gestisce utenti vede anche l'audit log.
@RestController
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService auditQueryService;

    @GetMapping("/api/logs")
    @RequirePerm(resource = "user", action = PermAction.READ)
    public ResponseEntity<List<AuditLog>> list(
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) String resourceId
    ) {
        return ResponseEntity.ok(auditQueryService.list(resource, resourceId));
    }
}
