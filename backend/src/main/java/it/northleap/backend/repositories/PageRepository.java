package it.northleap.backend.repositories;

import it.northleap.backend.entities.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PageRepository extends JpaRepository<Page, UUID> {
    Optional<Page> findByKey(String key);
    List<Page> findAllByOrderByLabelAsc();
}
