package it.northleap.backend.services;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Porting di WorkflowScheduler (workflows.module.ts): polling orario su tutti i workflow con
// trigger "schedule", invece di registrare un cron Spring dedicato per ciascun workflow (stesso
// approccio "più semplice" indicato in 05-WORKFLOW-ENGINE.md - i cron dei workflow originali
// sono comunque a granularità giornaliera).
@Component
@RequiredArgsConstructor
public class WorkflowScheduler {

    private final WorkflowEngine workflowEngine;

    @Scheduled(cron = "0 0 * * * *")
    public void handle() {
        workflowEngine.runScheduled();
    }
}
