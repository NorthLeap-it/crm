package it.northleap.backend.dtos;

import it.northleap.backend.entities.Workflow;
import it.northleap.backend.entities.WorkflowRun;

import java.util.List;

// stesso pattern compositivo di RecordDetailResponse (Fase 3) per evitare un @OneToMany
// superfluo su Workflow: le runs vengono composte qui via WorkflowRunRepository, non come
// collezione di entità.
public record WorkflowDetailResponse(Workflow workflow, List<WorkflowRun> runs) {
}
