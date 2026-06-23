package it.northleap.backend.services;

import it.northleap.backend.dtos.UpdateWorkspaceDto;
import it.northleap.backend.dtos.WorkspaceResponse;
import it.northleap.backend.entities.Workspace;
import it.northleap.backend.exceptions.NotFoundException;
import it.northleap.backend.repositories.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    private WorkspaceService newService() {
        return new WorkspaceService(workspaceRepository);
    }

    private Workspace existingWorkspace() {
        Workspace ws = new Workspace();
        ws.setName("NorthLeap");
        ws.setBrandColor("#0A84FF");
        return ws;
    }

    @Test
    void getMapsTheCurrentWorkspace() {
        when(workspaceRepository.findTopByOrderByCreatedAtAsc()).thenReturn(Optional.of(existingWorkspace()));

        WorkspaceResponse result = newService().get();

        assertEquals("NorthLeap", result.name());
        assertEquals("#0A84FF", result.brandColor());
    }

    @Test
    void updateRenamesAndPersists() {
        Workspace ws = existingWorkspace();
        when(workspaceRepository.findTopByOrderByCreatedAtAsc()).thenReturn(Optional.of(ws));

        UpdateWorkspaceDto dto = new UpdateWorkspaceDto();
        dto.setName("Acme Inc.");
        WorkspaceResponse result = newService().update(dto);

        assertEquals("Acme Inc.", ws.getName());
        assertEquals("Acme Inc.", result.name());
        // brandColor non fornito -> resta invariato
        assertEquals("#0A84FF", ws.getBrandColor());
        verify(workspaceRepository).save(ws);
    }

    @Test
    void updateOnlyTouchesBrandFieldsWhenProvided() {
        Workspace ws = existingWorkspace();
        when(workspaceRepository.findTopByOrderByCreatedAtAsc()).thenReturn(Optional.of(ws));

        UpdateWorkspaceDto dto = new UpdateWorkspaceDto();
        dto.setName("Acme Inc.");
        dto.setBrandColor("#FF0000");
        dto.setLogoUrl("https://example.com/logo.png");
        newService().update(dto);

        assertEquals("#FF0000", ws.getBrandColor());
        assertEquals("https://example.com/logo.png", ws.getLogoUrl());
    }

    @Test
    void throwsWhenNoWorkspaceConfigured() {
        when(workspaceRepository.findTopByOrderByCreatedAtAsc()).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> newService().get());
    }
}
