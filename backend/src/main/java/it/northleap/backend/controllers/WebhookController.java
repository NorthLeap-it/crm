package it.northleap.backend.controllers;

import it.northleap.backend.dtos.CreateWebhookDto;
import it.northleap.backend.dtos.WebhookSummary;
import it.northleap.backend.entities.Webhook;
import it.northleap.backend.security.PermAction;
import it.northleap.backend.security.RequirePerm;
import it.northleap.backend.services.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Porting di WebhooksController (webhooks.module.ts). "workflow" come risorsa RBAC per gli
// endpoint di gestione e' un quirk esplicito dell'originale, mantenuto per fedeltà (stesso
// schema già usato da ObjectsController con "page"). POST /in/{id} e' pubblico - aggiunto al
// matcher di SecurityConfig - protetto dalla verifica HMAC dentro WebhookService, non da
// Bearer/ApiKey: chi chiama e' un sistema esterno.
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @GetMapping
    @RequirePerm(resource = "workflow", action = PermAction.READ)
    public ResponseEntity<List<WebhookSummary>> list() {
        return ResponseEntity.ok(webhookService.list());
    }

    @PostMapping
    @RequirePerm(resource = "workflow", action = PermAction.WRITE)
    public ResponseEntity<Webhook> create(@Valid @RequestBody CreateWebhookDto dto) {
        return ResponseEntity.ok(webhookService.create(dto));
    }

    @DeleteMapping("/{id}")
    @RequirePerm(resource = "workflow", action = PermAction.WRITE)
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        webhookService.remove(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/in/{id}")
    public ResponseEntity<Map<String, Object>> receive(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(webhookService.receive(id, signature, body));
    }
}
