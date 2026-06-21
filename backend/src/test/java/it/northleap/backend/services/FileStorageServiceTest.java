package it.northleap.backend.services;

import it.northleap.backend.entities.FileObject;
import it.northleap.backend.exceptions.BadRequestException;
import it.northleap.backend.exceptions.NotFoundException;
import it.northleap.backend.repositories.FileObjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private FileObjectRepository fileObjectRepository;

    private FileStorageService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new FileStorageService(fileObjectRepository);
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
    }

    @Test
    void storeGeneratesRandomNamePreservingExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", "contenuto".getBytes());
        UUID actorId = UUID.randomUUID();

        FileObject saved = service.store(file, actorId, null);

        assertEquals("report.pdf", saved.getFilename());
        assertTrue(saved.getPath().endsWith(".pdf"));
        assertTrue(!saved.getPath().equals("report.pdf"), "il nome su disco deve essere randomizzato, non l'originale");
        assertEquals(actorId, saved.getUploadedBy());
        verify(fileObjectRepository).save(saved);
    }

    @Test
    void resolveOnDiskRejectsPathOutsideUploadDir() {
        FileObject malicious = new FileObject();
        malicious.setPath("../../etc/passwd");

        assertThrows(BadRequestException.class, () -> service.resolveOnDisk(malicious));
    }

    @Test
    void resolveOnDiskAcceptsNormalPath() {
        FileObject file = new FileObject();
        file.setPath("abc123.pdf");

        Path resolved = service.resolveOnDisk(file);

        assertTrue(resolved.startsWith(tempDir));
    }

    @Test
    void getThrowsWhenFileNotFound() {
        UUID id = UUID.randomUUID();
        when(fileObjectRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.get(id));
    }
}
