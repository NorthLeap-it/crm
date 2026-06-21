package it.northleap.backend.services;

import it.northleap.backend.entities.Notification;
import it.northleap.backend.repositories.NotificationRepository;
import it.northleap.backend.security.Actor;
import it.northleap.backend.security.ActorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService service;
    private Actor actor;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository);
        actor = new Actor(UUID.randomUUID(), ActorType.USER, "u@test.com", List.of());
    }

    @Test
    void markReadOnlyAffectsOwnNotification() {
        Notification mine = new Notification();
        mine.setId(UUID.randomUUID());
        mine.setUserId(actor.id());
        when(notificationRepository.findById(mine.getId())).thenReturn(Optional.of(mine));

        service.markRead(actor, mine.getId());

        assertNotNull(mine.getReadAt());
        verify(notificationRepository).save(mine);
    }

    @Test
    void markReadIsNoOpForSomeoneElsesNotification() {
        Notification someoneElses = new Notification();
        someoneElses.setId(UUID.randomUUID());
        someoneElses.setUserId(UUID.randomUUID());
        when(notificationRepository.findById(someoneElses.getId())).thenReturn(Optional.of(someoneElses));

        service.markRead(actor, someoneElses.getId());

        assertNull(someoneElses.getReadAt());
        verify(notificationRepository, never()).save(someoneElses);
    }

    @Test
    void markAllReadOnlyUpdatesUnreadOnes() {
        Notification unread1 = new Notification();
        Notification unread2 = new Notification();
        when(notificationRepository.findByUserIdAndReadAtIsNull(actor.id())).thenReturn(List.of(unread1, unread2));

        service.markAllRead(actor);

        assertNotNull(unread1.getReadAt());
        assertNotNull(unread2.getReadAt());
        verify(notificationRepository).saveAll(List.of(unread1, unread2));
    }
}
