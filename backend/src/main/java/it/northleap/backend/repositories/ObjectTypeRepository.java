package it.northleap.backend.repositories;

import it.northleap.backend.entities.ObjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ObjectTypeRepository extends JpaRepository<ObjectType, UUID> {
    Optional<ObjectType> findByKey(String key);
    List<ObjectType> findAllByOrderBySortOrderAsc();
}
