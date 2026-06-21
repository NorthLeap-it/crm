package it.northleap.backend.controllers;

import it.northleap.backend.dtos.CreateWorkflowDto;
import it.northleap.backend.dtos.RunQueuedResponse;
import it.northleap.backend.dtos.RunWorkflowRequest;
import it.northleap.backend.dtos.UpdateWorkflowDto;
import it.northleap.backend.dtos.WorkflowDetailResponse;
import it.northleap.backend.entities.Workflow;
import it.northleap.backend.security.PermAction;
import it.northleap.backend.security.RequirePerm;
import it.northleap.backend.services.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Porting di WorkflowsController (workflows.module.ts). "workflow" e' gia' una delle 5 risorse
// fisse seminate da PermissionSeeder (Fase 2/3), nessuna modifica al seeder necessaria.
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping
    @RequirePerm(resource = "workflow", action = PermAction.READ)
    public ResponseEntity<List<Workflow>> list() {
        return ResponseEntity.ok(workflowService.list());
    }

    @GetMapping("/{id}")
    @RequirePerm(resource = "workflow", action = PermAction.READ)
    public ResponseEntity<WorkflowDetailResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.get(id));
    }

    @PostMapping
    @RequirePerm(resource = "workflow", action = PermAction.WRITE)
    public ResponseEntity<Workflow> create(@Valid @RequestBody CreateWorkflowDto dto) {
        return ResponseEntity.ok(workflowService.create(dto));
    }

    @PatchMapping("/{id}")
    @RequirePerm(resource = "workflow", action = PermAction.WRITE)
    public ResponseEntity<Workflow> update(@PathVariable UUID id, @RequestBody UpdateWorkflowDto dto) {
        return ResponseEntity.ok(workflowService.update(id, dto));
    }

    // la X di RBAC, esplicitamente richiesta da 05-WORKFLOW-ENGINE.md per l'esecuzione manuale
    @PostMapping("/{id}/run")
    @RequirePerm(resource = "workflow", action = PermAction.EXECUTE)
    public ResponseEntity<RunQueuedResponse> run(@PathVariable UUID id, @RequestBody(required = false) RunWorkflowRequest body) {
        UUID recordId = body != null ? body.getRecordId() : null;
        return ResponseEntity.ok(workflowService.run(id, recordId));
    }

    @DeleteMapping("/{id}")
    @RequirePerm(resource = "workflow", action = PermAction.WRITE)
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        workflowService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
