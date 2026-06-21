package it.northleap.backend.services;

import it.northleap.backend.dtos.CreateWorkflowDto;
import it.northleap.backend.dtos.RunQueuedResponse;
import it.northleap.backend.dtos.UpdateWorkflowDto;
import it.northleap.backend.dtos.WorkflowDetailResponse;
import it.northleap.backend.entities.Workflow;
import it.northleap.backend.exceptions.NotFoundException;
import it.northleap.backend.repositories.WorkflowRepository;
import it.northleap.backend.repositories.WorkflowRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// Porting di WorkflowsService (workflows.module.ts).
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowEngine workflowEngine;

    public List<Workflow> list() {
        return workflowRepository.findAllByOrderByCreatedAtDesc();
    }

    public WorkflowDetailResponse get(UUID id) {
        Workflow wf = workflowOrThrow(id);
        return new WorkflowDetailResponse(wf, workflowRunRepository.findTop20ByWorkflow_IdOrderByStartedAtDesc(id));
    }

    @Transactional
    public Workflow create(CreateWorkflowDto dto) {
        Workflow wf = new Workflow();
        wf.setName(dto.getName());
        wf.setDescription(dto.getDescription());
        wf.setTrigger(dto.getTrigger());
        wf.setConditions(dto.getConditions());
        wf.setActions(dto.getActions() != null ? dto.getActions() : List.of());
        wf.setGraph(dto.getGraph());
        wf.setActive(dto.getIsActive() == null || dto.getIsActive());
        workflowRepository.save(wf);
        return wf;
    }

    @Transactional
    public Workflow update(UUID id, UpdateWorkflowDto dto) {
        Workflow wf = workflowOrThrow(id);
        if (dto.getName() != null) wf.setName(dto.getName());
        if (dto.getDescription() != null) wf.setDescription(dto.getDescription());
        if (dto.getTrigger() != null) wf.setTrigger(dto.getTrigger());
        if (dto.getConditions() != null) wf.setConditions(dto.getConditions());
        if (dto.getActions() != null) wf.setActions(dto.getActions());
        if (dto.getGraph() != null) wf.setGraph(dto.getGraph());
        if (dto.getIsActive() != null) wf.setActive(dto.getIsActive());
        workflowRepository.save(wf);
        return wf;
    }

    @Transactional
    public void remove(UUID id) {
        Workflow wf = workflowOrThrow(id);
        workflowRepository.delete(wf);
    }

    public RunQueuedResponse run(UUID id, UUID recordId) {
        workflowEngine.runManual(id, recordId);
        return new RunQueuedResponse(true);
    }

    private Workflow workflowOrThrow(UUID id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Workflow non trovato"));
    }
}
