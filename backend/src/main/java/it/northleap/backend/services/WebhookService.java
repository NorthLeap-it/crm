package it.northleap.backend.services;

import it.northleap.backend.dtos.CreateWebhookDto;
import it.northleap.backend.dtos.WebhookSummary;
import it.northleap.backend.entities.Webhook;
import it.northleap.backend.entities.WebhookDirection;
import it.northleap.backend.events.WebhookReceivedEvent;
import it.northleap.backend.exceptions.NotFoundException;
import it.northleap.backend.exceptions.RbacDeniedException;
import it.northleap.backend.repositories.WebhookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Porting di WebhooksService (webhooks.module.ts).
@Service
@RequiredArgsConstructor
public class WebhookService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final WebhookRepository webhookRepository;
    private final ApplicationEventPublisher events;
    private final ObjectMapper objectMapper;

    // niente secret in lista (usato per HMAC, deve restare conoscibile solo a chi l'ha
    // appena creato il webhook, vedi WebhookSummary) - l'originale lo espone anche qui, deviazione
    // di hardening deliberata
    public List<WebhookSummary> list() {
        return webhookRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(w -> new WebhookSummary(w.getId(), w.getDirection(), w.getName(), w.getUrl(),
                        w.getEvents(), w.isActive(), w.getCreatedAt()))
                .toList();
    }

    @Transactional
    public Webhook create(CreateWebhookDto dto) {
        Webhook webhook = new Webhook();
        webhook.setDirection(dto.getDirection());
        webhook.setName(dto.getName());
        webhook.setUrl(dto.getUrl());
        webhook.setEvents(dto.getEvents());
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        webhook.setSecret(HexFormat.of().formatHex(bytes));
        webhookRepository.save(webhook);
        return webhook;
    }

    public void remove(UUID id) {
        webhookRepository.deleteById(id);
    }

    public Map<String, Object> receive(UUID id, String signature, Map<String, Object> rawBody) {
        Webhook hook = webhookRepository.findById(id)
                .filter(w -> w.getDirection() == WebhookDirection.INBOUND && w.isActive())
                .orElseThrow(() -> new NotFoundException("Webhook non trovato"));

        // stesso schema dell'originale: ri-serializza il body già deserializzato e firma quello
        // (stessa fragilità nota dell'originale se mittente e ricevente serializzano in modo
        // diverso - non corretta qui, fedeltà di porting)
        String bodyJson = objectMapper.writeValueAsString(rawBody);
        String expected = HmacUtil.sha256Hex(hook.getSecret(), bodyJson);

        // RbacDeniedException riusata per il suo esito HTTP (403 Forbidden con messaggio), non
        // perché questo sia un caso RBAC - nessun'altra eccezione 403 generica esiste nel
        // progetto e non vale la pena introdurne una per un solo punto d'uso. Confronto a tempo
        // costante (MessageDigest.isEqual) invece di un confronto diretto come l'originale:
        // hardening minimo dichiarato, stesso esito osservabile.
        if (signature == null || !MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
            throw new RbacDeniedException("Firma non valida");
        }

        events.publishEvent(new WebhookReceivedEvent(id, rawBody));
        return Map.of("received", true);
    }
}
