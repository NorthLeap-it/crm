package it.northleap.backend.services;

import it.northleap.backend.entities.Webhook;
import it.northleap.backend.entities.WebhookDirection;
import it.northleap.backend.events.WebhookReceivedEvent;
import it.northleap.backend.exceptions.NotFoundException;
import it.northleap.backend.exceptions.RbacDeniedException;
import it.northleap.backend.repositories.WebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookRepository webhookRepository;
    @Mock
    private ApplicationEventPublisher events;

    private WebhookService service;

    @BeforeEach
    void setUp() {
        service = new WebhookService(webhookRepository, events, JsonMapper.builder().build());
    }

    private Webhook inboundWebhook(String secret) {
        Webhook w = new Webhook();
        w.setId(UUID.randomUUID());
        w.setDirection(WebhookDirection.INBOUND);
        w.setSecret(secret);
        w.setActive(true);
        return w;
    }

    @Test
    void receiveAcceptsValidSignature() {
        Webhook hook = inboundWebhook("topsecret");
        when(webhookRepository.findById(hook.getId())).thenReturn(Optional.of(hook));
        Map<String, Object> body = Map.of("event", "test");
        String json = JsonMapper.builder().build().writeValueAsString(body);
        String validSignature = HmacUtil.sha256Hex("topsecret", json);

        Map<String, Object> result = service.receive(hook.getId(), validSignature, body);

        assertEquals(true, result.get("received"));
        verify(events).publishEvent(new WebhookReceivedEvent(hook.getId(), body));
    }

    @Test
    void receiveRejectsInvalidSignature() {
        Webhook hook = inboundWebhook("topsecret");
        when(webhookRepository.findById(hook.getId())).thenReturn(Optional.of(hook));

        assertThrows(RbacDeniedException.class, () -> service.receive(hook.getId(), "wrong-signature", Map.of("event", "test")));
        verify(events, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void receiveRejectsMissingSignature() {
        Webhook hook = inboundWebhook("topsecret");
        when(webhookRepository.findById(hook.getId())).thenReturn(Optional.of(hook));

        assertThrows(RbacDeniedException.class, () -> service.receive(hook.getId(), null, Map.of("event", "test")));
    }

    @Test
    void receiveRejectsOutboundWebhook() {
        Webhook hook = inboundWebhook("topsecret");
        hook.setDirection(WebhookDirection.OUTBOUND);
        when(webhookRepository.findById(hook.getId())).thenReturn(Optional.of(hook));

        assertThrows(NotFoundException.class, () -> service.receive(hook.getId(), "anything", Map.of()));
    }

    @Test
    void receiveRejectsInactiveWebhook() {
        Webhook hook = inboundWebhook("topsecret");
        hook.setActive(false);
        when(webhookRepository.findById(hook.getId())).thenReturn(Optional.of(hook));

        assertThrows(NotFoundException.class, () -> service.receive(hook.getId(), "anything", Map.of()));
    }

    @Test
    void receiveRejectsUnknownWebhook() {
        UUID id = UUID.randomUUID();
        when(webhookRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.receive(id, "anything", Map.of()));
    }
}
