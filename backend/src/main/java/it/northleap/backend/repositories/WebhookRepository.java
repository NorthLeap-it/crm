package it.northleap.backend.repositories;

import it.northleap.backend.entities.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, UUID> {
    List<Webhook> findAllByOrderByCreatedAtDesc();
}
