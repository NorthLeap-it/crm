package it.northleap.backend.controllers;

import it.northleap.backend.dtos.ApiKeyCreatedResponse;
import it.northleap.backend.dtos.ApiKeySummary;
import it.northleap.backend.dtos.CreateApiKeyDto;
import it.northleap.backend.security.PermAction;
import it.northleap.backend.security.RequirePerm;
import it.northleap.backend.services.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Porting di ApiKeysController (api-keys.module.ts).
@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    @RequirePerm(resource = "apikey", action = PermAction.READ)
    public ResponseEntity<List<ApiKeySummary>> list() {
        return ResponseEntity.ok(apiKeyService.list());
    }

    @PostMapping
    @RequirePerm(resource = "apikey", action = PermAction.WRITE)
    public ResponseEntity<ApiKeyCreatedResponse> create(@Valid @RequestBody CreateApiKeyDto dto) {
        return ResponseEntity.ok(apiKeyService.create(dto));
    }

    @DeleteMapping("/{id}")
    @RequirePerm(resource = "apikey", action = PermAction.WRITE)
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        apiKeyService.revoke(id);
        return ResponseEntity.noContent().build();
    }
}
