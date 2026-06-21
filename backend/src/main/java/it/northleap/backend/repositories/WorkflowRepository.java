package it.northleap.backend.repositories;

import it.northleap.backend.entities.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    List<Workflow> findAllByOrderByCreatedAtDesc();
    List<Workflow> findByIsActiveTrue();
    Optional<Workflow> findByName(String name);
}
