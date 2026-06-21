package it.northleap.backend.repositories;

import it.northleap.backend.entities.FileObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileObjectRepository extends JpaRepository<FileObject, UUID> {
}
