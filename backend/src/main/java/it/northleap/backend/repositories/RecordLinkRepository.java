package it.northleap.backend.repositories;

import it.northleap.backend.entities.RecordLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecordLinkRepository extends JpaRepository<RecordLink, UUID> {
    List<RecordLink> findBySource_Id(UUID sourceId);
    List<RecordLink> findByTarget_Id(UUID targetId);
    Optional<RecordLink> findBySource_IdAndTarget_IdAndRelationKey(UUID sourceId, UUID targetId, String relationKey);
}
