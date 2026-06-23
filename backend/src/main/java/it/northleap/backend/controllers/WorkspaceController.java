package it.northleap.backend.controllers;

import it.northleap.backend.dtos.UpdateWorkspaceDto;
import it.northleap.backend.dtos.WorkspaceResponse;
import it.northleap.backend.security.PermAction;
import it.northleap.backend.security.RequirePerm;
import it.northleap.backend.services.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Lettura/modifica del Workspace (nome azienda, brand). La lettura e' libera a ogni utente
// autenticato (la topbar mostra il nome a tutti); la modifica e' riservata a chi gestisce
// l'organizzazione - gata sulla risorsa RBAC "user", la stessa che l'audit log riusa col senso
// "chi amministra gli utenti amministra anche l'organizzazione".
@RestController
@RequestMapping("/api/workspace")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping
    public ResponseEntity<WorkspaceResponse> get() {
        return ResponseEntity.ok(workspaceService.get());
    }

    @PatchMapping
    @RequirePerm(resource = "user", action = PermAction.WRITE)
    public ResponseEntity<WorkspaceResponse> update(@Valid @RequestBody UpdateWorkspaceDto dto) {
        return ResponseEntity.ok(workspaceService.update(dto));
    }
}
