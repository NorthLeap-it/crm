package it.northleap.backend.controllers;

import it.northleap.backend.entities.FileObject;
import it.northleap.backend.security.Actor;
import it.northleap.backend.security.CurrentActor;
import it.northleap.backend.services.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

// Porting di FilesController (files.module.ts). Niente @RequirePerm: l'originale non ne ha,
// l'upload è gated solo da autenticazione (anyRequest().authenticated() in SecurityConfig).
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileObject> upload(
            @CurrentActor Actor actor,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "recordId", required = false) UUID recordId
    ) {
        UUID actorId = actor != null ? actor.id() : null;
        return ResponseEntity.ok(fileStorageService.store(file, actorId, recordId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        FileObject file = fileStorageService.get(id);
        Resource resource = new FileSystemResource(fileStorageService.resolveOnDisk(file));

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.getFilename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(file.getMime() != null ? MediaType.parseMediaType(file.getMime()) : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
