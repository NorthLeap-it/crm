package it.northleap.backend.controllers;

import it.northleap.backend.entities.Notification;
import it.northleap.backend.security.Actor;
import it.northleap.backend.security.CurrentActor;
import it.northleap.backend.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Porting di NotificationsController (notifications.module.ts). Niente @RequirePerm: sempre
// personali all'utente autenticato, coperte da anyRequest().authenticated().
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> list(@CurrentActor Actor actor) {
        return ResponseEntity.ok(notificationService.list(actor));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> readAll(@CurrentActor Actor actor) {
        notificationService.markAllRead(actor);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> read(@CurrentActor Actor actor, @PathVariable UUID id) {
        notificationService.markRead(actor, id);
        return ResponseEntity.noContent().build();
    }
}
