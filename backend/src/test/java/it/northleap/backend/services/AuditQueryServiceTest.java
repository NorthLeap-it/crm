package it.northleap.backend.services;

import it.northleap.backend.entities.AuditLog;
import it.northleap.backend.repositories.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditQueryService service;

    @BeforeEach
    void setUp() {
        service = new AuditQueryService(auditLogRepository);
    }

    @Test
    void usesBothFiltersWhenResourceAndResourceIdProvided() {
        when(auditLogRepository.findTop100ByResourceAndResourceIdOrderByCreatedAtDesc("record", "123"))
                .thenReturn(List.of(new AuditLog()));

        List<AuditLog> result = service.list("record", "123");

        assertEquals(1, result.size());
        verify(auditLogRepository).findTop100ByResourceAndResourceIdOrderByCreatedAtDesc("record", "123");
        verifyNoMoreInteractions(auditLogRepository);
    }

    @Test
    void usesResourceOnlyFilter() {
        when(auditLogRepository.findTop100ByResourceOrderByCreatedAtDesc("record")).thenReturn(List.of());

        service.list("record", null);

        verify(auditLogRepository).findTop100ByResourceOrderByCreatedAtDesc("record");
        verifyNoMoreInteractions(auditLogRepository);
    }

    @Test
    void usesResourceIdOnlyFilter() {
        when(auditLogRepository.findTop100ByResourceIdOrderByCreatedAtDesc("123")).thenReturn(List.of());

        service.list(null, "123");

        verify(auditLogRepository).findTop100ByResourceIdOrderByCreatedAtDesc("123");
        verifyNoMoreInteractions(auditLogRepository);
    }

    @Test
    void usesNoFilterWhenBothAbsent() {
        when(auditLogRepository.findTop100ByOrderByCreatedAtDesc()).thenReturn(List.of());

        service.list(null, null);

        verify(auditLogRepository).findTop100ByOrderByCreatedAtDesc();
        verifyNoMoreInteractions(auditLogRepository);
    }
}
