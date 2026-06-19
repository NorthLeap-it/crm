package it.northleap.backend.controllers;

import it.northleap.backend.dtos.CreateObjectTypeDto;
import it.northleap.backend.dtos.FieldDefDto;
import it.northleap.backend.dtos.UpdateObjectTypeDto;
import it.northleap.backend.entities.ObjectType;
import it.northleap.backend.security.PermAction;
import it.northleap.backend.security.RequirePerm;
import it.northleap.backend.services.ObjectsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Porting di objects.controller.ts. Stessa scelta (quirk) dell'originale: la gestione di
// ObjectType/FieldDef è gated dalla risorsa RBAC "page", non da una risorsa dedicata.
@RestController
@RequestMapping("/api/objects")
@RequiredArgsConstructor
public class ObjectsController {

    private final ObjectsService objectsService;

    @GetMapping
    @RequirePerm(resource = "page", action = PermAction.READ)
    public ResponseEntity<List<ObjectType>> list() {
        return ResponseEntity.ok(objectsService.list());
    }

    @GetMapping("/{key}")
    @RequirePerm(resource = "page", action = PermAction.READ)
    public ResponseEntity<ObjectType> get(@PathVariable String key) {
        return ResponseEntity.ok(objectsService.get(key));
    }

    @PostMapping
    @RequirePerm(resource = "page", action = PermAction.WRITE)
    public ResponseEntity<ObjectType> create(@Valid @RequestBody CreateObjectTypeDto dto) {
        return ResponseEntity.ok(objectsService.create(dto));
    }

    @PatchMapping("/{key}")
    @RequirePerm(resource = "page", action = PermAction.WRITE)
    public ResponseEntity<ObjectType> update(@PathVariable String key, @RequestBody UpdateObjectTypeDto dto) {
        return ResponseEntity.ok(objectsService.update(key, dto));
    }

    @DeleteMapping("/{key}")
    @RequirePerm(resource = "page", action = PermAction.WRITE)
    public ResponseEntity<Void> remove(@PathVariable String key) {
        objectsService.remove(key);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{key}/fields")
    @RequirePerm(resource = "page", action = PermAction.WRITE)
    public ResponseEntity<ObjectType> addField(@PathVariable String key, @Valid @RequestBody FieldDefDto dto) {
        return ResponseEntity.ok(objectsService.addField(key, dto));
    }

    @PatchMapping("/{key}/fields/{fieldKey}")
    @RequirePerm(resource = "page", action = PermAction.WRITE)
    public ResponseEntity<ObjectType> updateField(
            @PathVariable String key,
            @PathVariable String fieldKey,
            @RequestBody FieldDefDto dto
    ) {
        return ResponseEntity.ok(objectsService.updateField(key, fieldKey, dto));
    }

    @DeleteMapping("/{key}/fields/{fieldKey}")
    @RequirePerm(resource = "page", action = PermAction.WRITE)
    public ResponseEntity<ObjectType> removeField(@PathVariable String key, @PathVariable String fieldKey) {
        return ResponseEntity.ok(objectsService.removeField(key, fieldKey));
    }
}
