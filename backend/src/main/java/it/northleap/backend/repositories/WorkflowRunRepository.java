package it.northleap.backend.repositories;

import it.northleap.backend.entities.WorkflowRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, UUID> {
    List<WorkflowRun> findTop20ByWorkflow_IdOrderByStartedAtDesc(UUID workflowId);
}
