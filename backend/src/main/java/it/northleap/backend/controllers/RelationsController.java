package it.northleap.backend.controllers;

import it.northleap.backend.dtos.CreateLinkDto;
import it.northleap.backend.entities.RecordLink;
import it.northleap.backend.security.Actor;
import it.northleap.backend.security.CurrentActor;
import it.northleap.backend.services.RelationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Porting di RelationsController (relations.module.ts). Nidificato sotto /api/records: i path
// letterali ("/links") vincono sui path-variable nel routing di Spring, nessuna collisione con
// RecordsController's /{key}/{id}.
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RelationsController {

    private final RelationsService relationsService;

    @GetMapping("/{recordId}/links")
    public ResponseEntity<List<RecordLink>> list(@PathVariable UUID recordId) {
        return ResponseEntity.ok(relationsService.list(recordId));
    }

    @PostMapping("/{recordId}/links")
    public ResponseEntity<RecordLink> create(
            @CurrentActor Actor actor,
            @PathVariable UUID recordId,
            @Valid @RequestBody CreateLinkDto dto
    ) {
        return ResponseEntity.ok(relationsService.create(actor, recordId, dto));
    }

    // recordId non è usato nel body del metodo: stessa forma URL a 3 segmenti dell'originale,
    // che a sua volta non lo legge nel relativo handler
    @DeleteMapping("/{recordId}/links/{linkId}")
    public ResponseEntity<Void> remove(@CurrentActor Actor actor, @PathVariable UUID recordId, @PathVariable UUID linkId) {
        relationsService.remove(actor, linkId);
        return ResponseEntity.noContent().build();
    }
}
