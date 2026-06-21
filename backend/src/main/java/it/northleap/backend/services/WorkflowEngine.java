package it.northleap.backend.services;

import it.northleap.backend.entities.ObjectType;
import it.northleap.backend.entities.Record;
import it.northleap.backend.entities.Workflow;
import it.northleap.backend.entities.WorkflowRun;
import it.northleap.backend.entities.WorkflowRunStatus;
import it.northleap.backend.events.RecordCreatedEvent;
import it.northleap.backend.events.RecordDeletedEvent;
import it.northleap.backend.events.RecordUpdatedEvent;
import it.northleap.backend.repositories.ObjectTypeRepository;
import it.northleap.backend.repositories.RecordRepository;
import it.northleap.backend.repositories.WorkflowRepository;
import it.northleap.backend.repositories.WorkflowRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Porting dell'orchestratore di workflow.engine.ts. L'originale gira su BullMQ (coda Redis +
// worker separato); qui sostituiamo la coda con @Async + un TaskExecutor dedicato
// (config/AsyncConfig.java), come proposto da 05-WORKFLOW-ENGINE.md per l'MVP - limite
// consapevole: nessuna persistenza della coda, un job non ancora preso da un thread si perde se
// l'app si riavvia.
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngine {

    private final WorkflowRepository workflowRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final ObjectTypeRepository objectTypeRepository;
    private final RecordRepository recordRepository;
    private final ConditionEvaluator conditionEvaluator;
    private final GraphWorkflowRunner graphWorkflowRunner;
    private final WorkflowActionExecutor workflowActionExecutor;

    @EventListener
    public void onCreated(RecordCreatedEvent e) {
        dispatch("record.created", e.objectKey(), e.record(), null, null);
    }

    @EventListener
    public void onUpdated(RecordUpdatedEvent e) {
        dispatch("record.updated", e.objectKey(), e.record(), e.before(), e.beforeStatus());
    }

    @EventListener
    public void onDeleted(RecordDeletedEvent e) {
        dispatch("record.deleted", e.objectKey(), e.record(), null, null);
    }

    @SuppressWarnings("unchecked")
    private void dispatch(String eventType, String objectKey, Record record, Map<String, Object> before, String beforeStatus) {
        for (Workflow wf : workflowRepository.findByIsActiveTrue()) {
            Map<String, Object> trigger = wf.getTrigger();
            String triggerType = String.valueOf(trigger.get("type"));
            boolean typeMatches = triggerType.equals(eventType)
                    || ("record.updated".equals(eventType) && "field.changed".equals(triggerType));
            if (!typeMatches) {
                continue;
            }
            Object triggerObjectKey = trigger.get("objectKey");
            if (triggerObjectKey != null && !triggerObjectKey.equals(objectKey)) {
                continue;
            }
            if ("field.changed".equals(triggerType) && trigger.get("field") != null) {
                String field = String.valueOf(trigger.get("field"));
                Object a = before != null ? before.get(field) : null;
                Object b = record != null && record.getData() != null ? record.getData().get(field) : null;
                boolean dataFieldUnchanged = Objects.equals(a, b);
                boolean statusUnchanged = Objects.equals(beforeStatus, record != null ? record.getStatus() : null);
                if (dataFieldUnchanged && statusUnchanged) {
                    continue;
                }
            }
            runAsync(wf.getId(), record);
        }
    }

    /** Trigger schedulati (chiamato dallo scheduler orario). */
    public void runScheduled() {
        for (Workflow wf : workflowRepository.findByIsActiveTrue()) {
            Map<String, Object> trigger = wf.getTrigger();
            if (!"schedule".equals(String.valueOf(trigger.get("type")))) {
                continue;
            }
            Object objectKey = trigger.get("objectKey");
            List<Record> records;
            if (objectKey != null) {
                ObjectType obj = objectTypeRepository.findByKey(String.valueOf(objectKey)).orElse(null);
                records = obj != null ? recordRepository.findByObjectType_IdAndIsDeletedFalse(obj.getId()) : List.of();
            } else {
                records = new ArrayList<>();
                records.add(null);
            }
            for (Record record : records) {
                runAsync(wf.getId(), record);
            }
        }
    }

    /** Esecuzione manuale di un workflow su un record. */
    public Map<String, Object> runManual(UUID workflowId, UUID recordId) {
        Record record = recordId != null ? recordRepository.findById(recordId).orElse(null) : null;
        runAsync(workflowId, record);
        return Map.of("queued", true);
    }

    @Async("workflowTaskExecutor")
    public void runAsync(UUID workflowId, Record record) {
        runWorkflow(workflowId, record);
    }

    // seam di testabilità: i test chiamano questo metodo direttamente, bypassando il proxy
    // @Async, stesso trucco già usato per AnalyticsService con l'overload a referenceDate.
    // Niente @Transactional qui di proposito: runAsync chiama questo metodo come
    // self-invocation (stessa classe, niente proxy Spring in mezzo, quindi un eventuale
    // @Transactional verrebbe comunque ignorato silenziosamente) - ma è anche la cosa giusta a
    // livello logico, ogni save() di WorkflowRun deve poter commitare il proprio stato
    // (RUNNING poi SUCCESS/FAILED) separatamente, non in un'unica transazione lunga che
    // includerebbe anche eventuali chiamate HTTP delle azioni
    void runWorkflow(UUID workflowId, Record record) {
        Workflow wf = workflowRepository.findById(workflowId).orElse(null);
        if (wf == null || !wf.isActive()) {
            return;
        }
        if (!conditionEvaluator.evaluate(wf.getConditions(), record)) {
            return;
        }

        WorkflowRun run = new WorkflowRun();
        run.setWorkflow(wf);
        run.setStatus(WorkflowRunStatus.RUNNING);
        run.setInput(Map.of("recordId", record != null && record.getId() != null ? record.getId().toString() : ""));
        run.setStartedAt(Instant.now());
        workflowRunRepository.save(run);

        List<Map<String, Object>> steps = new ArrayList<>();
        try {
            Map<String, Object> graph = wf.getGraph();
            if (graph != null && graph.get("nodes") instanceof List<?> nodes && !nodes.isEmpty()) {
                graphWorkflowRunner.run(graph, record, steps);
            } else if (wf.getActions() != null) {
                for (Map<String, Object> action : wf.getActions()) {
                    Map<String, Object> result = workflowActionExecutor.execute(action, record);
                    steps.add(Map.of("type", action.get("type"), "result", result));
                }
            }
            run.setStatus(WorkflowRunStatus.SUCCESS);
            run.setSteps(steps);
            run.setFinishedAt(Instant.now());
            workflowRunRepository.save(run);
        } catch (RuntimeException e) {
            run.setStatus(WorkflowRunStatus.FAILED);
            run.setSteps(steps);
            run.setError(e.getMessage());
            run.setFinishedAt(Instant.now());
            workflowRunRepository.save(run);
            // equivalente funzionale di worker.on('failed', ...) di BullMQ: qui non esiste un
            // worker separato a cui rilanciare, quindi il logging avviene sincronamente qui -
            // non rilanciamo oltre, a differenza dell'originale, perché non c'è un caller utile
            // ad attenderlo (siamo già dentro al thread @Async)
            log.error("Workflow {} fallito: {}", workflowId, e.getMessage(), e);
        }
    }
}
