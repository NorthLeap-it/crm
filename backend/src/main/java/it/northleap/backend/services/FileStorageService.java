package it.northleap.backend.services;

import it.northleap.backend.entities.FileObject;
import it.northleap.backend.exceptions.BadRequestException;
import it.northleap.backend.exceptions.NotFoundException;
import it.northleap.backend.repositories.FileObjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

// Porting di FilesService (files.module.ts). Stesso approccio dell'originale: disco locale,
// nome randomizzato (anti-collisione/anti-path-traversal), limite dimensione gestito a livello
// Spring (application.properties multipart.max-file-size), non qui.
@Service
@RequiredArgsConstructor
public class FileStorageService {

    // estensioni bloccate per default: l'originale non ha nessuna whitelist/blacklist (i file
    // sono solo salvati e riserviti come allegato, mai eseguiti server-side), ma una blocklist
    // dei tipi più ovviamente pericolosi è una difesa in profondità a costo quasi zero -
    // deviazione di hardening dichiarata, non fedeltà di porting
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            ".exe", ".dll", ".bat", ".cmd", ".sh", ".bash", ".ps1", ".msi", ".com", ".scr",
            ".php", ".php3", ".php4", ".php5", ".phtml", ".jsp", ".jspx", ".asp", ".aspx",
            ".jar", ".js", ".vbs", ".wsf", ".cgi", ".htaccess"
    );

    private final FileObjectRepository fileObjectRepository;

    @Value("${app.files.upload-dir:./uploads}")
    private String uploadDir;

    private Path uploadDirPath;

    private Path uploadDir() {
        if (uploadDirPath == null) {
            try {
                uploadDirPath = Path.of(uploadDir).toAbsolutePath().normalize();
                Files.createDirectories(uploadDirPath);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return uploadDirPath;
    }

    @Transactional
    public FileObject store(MultipartFile file, UUID actorId, UUID recordId) {
        String extension = extensionOf(file.getOriginalFilename());
        if (BLOCKED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException("Tipo di file non consentito: " + extension);
        }
        String diskName = UUID.randomUUID() + extension;
        Path target = uploadDir().resolve(diskName);

        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        FileObject entity = new FileObject();
        entity.setFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : diskName);
        entity.setMime(file.getContentType());
        entity.setSize(file.getSize());
        entity.setPath(diskName);
        entity.setUploadedBy(actorId);
        entity.setRecordId(recordId);
        fileObjectRepository.save(entity);
        return entity;
    }

    public FileObject get(UUID id) {
        return fileObjectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("File non trovato"));
    }

    // allegati di un record, dal più recente (per la sezione "Allegati" del dettaglio record)
    public java.util.List<FileObject> listByRecord(UUID recordId) {
        return fileObjectRepository.findByRecordIdOrderByCreatedAtDesc(recordId);
    }

    // guardia path-traversal esplicita: verifica che il path risolto resti dentro uploadDir.
    // L'originale si affida solo al fatto che il nome sia auto-generato; qui aggiungiamo un
    // controllo a runtime perché è economico e difensivo (deviazione di hardening dichiarata).
    public Path resolveOnDisk(FileObject file) {
        Path resolved = uploadDir().resolve(file.getPath()).normalize();
        if (!resolved.startsWith(uploadDir())) {
            throw new BadRequestException("Percorso file non valido");
        }
        return resolved;
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        // niente path-separator nell'estensione estratta (difesa aggiuntiva anti-traversal sul
        // nome file originale, che non tocca mai il path su disco se non per l'estensione)
        if (dot < 0 || originalFilename.indexOf('/', dot) >= 0 || originalFilename.indexOf('\\', dot) >= 0) {
            return "";
        }
        return originalFilename.substring(dot);
    }
}
