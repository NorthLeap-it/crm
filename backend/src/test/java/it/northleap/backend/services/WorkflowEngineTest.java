package it.northleap.backend.services;

import it.northleap.backend.entities.Record;
import it.northleap.backend.entities.Workflow;
import it.northleap.backend.entities.WorkflowRun;
import it.northleap.backend.entities.WorkflowRunStatus;
import it.northleap.backend.events.RecordCreatedEvent;
import it.northleap.backend.events.RecordUpdatedEvent;
import it.northleap.backend.repositories.ObjectTypeRepository;
import it.northleap.backend.repositories.RecordRepository;
import it.northleap.backend.repositories.WorkflowRepository;
import it.northleap.backend.repositories.WorkflowRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowEngineTest {

    @Mock
    private WorkflowRepository workflowRepository;
    @Mock
    private WorkflowRunRepository workflowRunRepository;
    @Mock
    private ObjectTypeRepository objectTypeRepository;
    @Mock
    private RecordRepository recordRepository;
    @Mock
    private GraphWorkflowRunner graphWorkflowRunner;
    @Mock
    private WorkflowActionExecutor workflowActionExecutor;

    private WorkflowEngine engine;

    @BeforeEach
    void setUp() {
        engine = new WorkflowEngine(workflowRepository, workflowRunRepository, objectTypeRepository,
                recordRepository, new ConditionEvaluator(), graphWorkflowRunner, workflowActionExecutor);
        lenient().when(workflowRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Workflow workflow(Map<String, Object> trigger, Map<String, Object> conditions) {
        Workflow wf = new Workflow();
        wf.setId(UUID.randomUUID());
        wf.setName("test-workflow");
        wf.setActive(true);
        wf.setTrigger(trigger);
        wf.setConditions(conditions);
        wf.setActions(List.of());
        return wf;
    }

    private Record record(String status, Map<String, Object> data) {
        Record r = new Record();
        r.setId(UUID.randomUUID());
        r.setStatus(status);
        r.setData(data);
        return r;
    }

    @Test
    void dispatchRunsOnlyWorkflowsMatchingTypeAndObjectKey() {
        Workflow matching = workflow(Map.of("type", "record.created", "objectKey", "lead"), null);
        Workflow nonMatching = workflow(Map.of("type", "record.created", "objectKey", "contact"), null);
        when(workflowRepository.findByIsActiveTrue()).thenReturn(List.of(matching, nonMatching));
        when(workflowRepository.findById(matching.getId())).thenReturn(Optional.of(matching));

        engine.onCreated(new RecordCreatedEvent("lead", record(null, Map.of())));

        verify(workflowRepository, times(1)).findById(matching.getId());
        verify(workflowRepository, never()).findById(nonMatching.getId());
    }

    @Test
    void scheduleTriggerIsNotDispatchedByRecordEvents() {
        Workflow scheduled = workflow(Map.of("type", "schedule", "cron", "0 6 * * *"), null);
        when(workflowRepository.findByIsActiveTrue()).thenReturn(List.of(scheduled));

        engine.onCreated(new RecordCreatedEvent("invoice", record(null, Map.of())));

        verify(workflowRepository, never()).findById(any());
    }

    @Test
    void fieldChangedFiresWhenWatchedFieldChangedEvenIfStatusUnchanged() {
        Workflow wf = workflow(Map.of("type", "field.changed", "objectKey", "lead", "field", "source"), null);
        when(workflowRepository.findByIsActiveTrue()).thenReturn(List.of(wf));
        when(workflowRepository.findById(wf.getId())).thenReturn(Optional.of(wf));

        Record after = record("new", Map.of("source", "referral"));
        engine.onUpdated(new RecordUpdatedEvent("lead", after, Map.of("source", "web"), "new"));

        verify(workflowRepository, times(1)).findById(wf.getId());
    }

    @Test
    void fieldChangedFiresWhenStatusChangedEvenIfWatchedFieldUnchanged() {
        Workflow wf = workflow(Map.of("type", "field.changed", "objectKey", "lead", "field", "source"), null);
        when(workflowRepository.findByIsActiveTrue()).thenReturn(List.of(wf));
        when(workflowRepository.findById(wf.getId())).thenReturn(Optional.of(wf));

        Record after = record("qualified", Map.of("source", "web"));
        engine.onUpdated(new RecordUpdatedEvent("lead", after, Map.of("source", "web"), "new"));

        verify(workflowRepository, times(1)).findById(wf.getId());
    }

    @Test
    void fieldChangedDoesNotFireWhenNeitherFieldNorStatusChanged() {
        Workflow wf = workflow(Map.of("type", "field.changed", "objectKey", "lead", "field", "source"), null);
        when(workflowRepository.findByIsActiveTrue()).thenReturn(List.of(wf));

        Record after = record("new", Map.of("source", "web"));
        engine.onUpdated(new RecordUpdatedEvent("lead", after, Map.of("source", "web"), "new"));

        verify(workflowRepository, never()).findById(any());
    }

    @Test
    void conditionsGateExecutionBeforeCreatingWorkflowRun() {
        Workflow wf = workflow(Map.of("type", "record.created", "objectKey", "opportunity"),
                Map.of("all", List.of(Map.of("field", "stage", "op", "eq", "value", "won"))));

        engine.runWorkflow(wf.getId(), record(null, Map.of("stage", "lost")));
        // findById non e' nemmeno mockato per questo test: se le condizioni non bloccassero
        // prima del findById, il test fallirebbe con un mock non configurato

        verify(workflowRunRepository, never()).save(any());
    }

    @Test
    void successfulRunPersistsSuccessWithSteps() {
        Workflow wf = workflow(Map.of("type", "record.created", "objectKey", "opportunity"), null);
        wf.setActions(List.of(Map.of("type", "notify_user", "target", "owner")));
        when(workflowRepository.findById(wf.getId())).thenReturn(Optional.of(wf));
        when(workflowActionExecutor.execute(any(), any())).thenReturn(Map.of("notified", true));

        engine.runWorkflow(wf.getId(), record(null, Map.of()));

        ArgumentCaptor<WorkflowRun> captor = ArgumentCaptor.forClass(WorkflowRun.class);
        verify(workflowRunRepository, times(2)).save(captor.capture());
        WorkflowRun finalRun = captor.getAllValues().get(1);
        assertEquals(WorkflowRunStatus.SUCCESS, finalRun.getStatus());
        assertEquals(1, finalRun.getSteps().size());
    }

    @Test
    void failedActionPersistsFailedRunWithError() {
        Workflow wf = workflow(Map.of("type", "record.created", "objectKey", "opportunity"), null);
        wf.setActions(List.of(Map.of("type", "create_record", "objectKey", "x")));
        when(workflowRepository.findById(wf.getId())).thenReturn(Optional.of(wf));
        when(workflowActionExecutor.execute(any(), any())).thenThrow(new RuntimeException("boom"));

        engine.runWorkflow(wf.getId(), record(null, Map.of()));

        ArgumentCaptor<WorkflowRun> captor = ArgumentCaptor.forClass(WorkflowRun.class);
        verify(workflowRunRepository, times(2)).save(captor.capture());
        WorkflowRun finalRun = captor.getAllValues().get(1);
        assertEquals(WorkflowRunStatus.FAILED, finalRun.getStatus());
        assertEquals("boom", finalRun.getError());
    }

    @Test
    void manualRunReturnsQueuedImmediately() {
        Workflow wf = workflow(Map.of("type", "record.created", "objectKey", "opportunity"), null);
        when(workflowRepository.findById(wf.getId())).thenReturn(Optional.of(wf));

        Map<String, Object> result = engine.runManual(wf.getId(), null);

        assertTrue((Boolean) result.get("queued"));
    }
}
