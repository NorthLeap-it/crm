package it.northleap.backend.services;

import it.northleap.backend.dtos.UpdateWorkspaceDto;
import it.northleap.backend.dtos.WorkspaceResponse;
import it.northleap.backend.entities.Workspace;
import it.northleap.backend.exceptions.NotFoundException;
import it.northleap.backend.repositories.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// App single-tenant: esiste un solo Workspace (creato all'onboarding, che si auto-blocca dopo il
// primo), quindi "il workspace" e' sempre il primo per data di creazione - stesso accesso usato
// da AuthService.isOnboarded().
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceResponse get() {
        return toResponse(currentWorkspace());
    }

    @Transactional
    public WorkspaceResponse update(UpdateWorkspaceDto dto) {
        Workspace workspace = currentWorkspace();
        workspace.setName(dto.getName());
        // brandColor/logoUrl: aggiornati solo se forniti, altrimenti restano com'erano
        if (dto.getBrandColor() != null) {
            workspace.setBrandColor(dto.getBrandColor());
        }
        if (dto.getLogoUrl() != null) {
            workspace.setLogoUrl(dto.getLogoUrl());
        }
        workspaceRepository.save(workspace);
        return toResponse(workspace);
    }

    private Workspace currentWorkspace() {
        return workspaceRepository.findTopByOrderByCreatedAtAsc()
                .orElseThrow(() -> new NotFoundException("Workspace non configurato"));
    }

    private WorkspaceResponse toResponse(Workspace workspace) {
        return new WorkspaceResponse(workspace.getName(), workspace.getBrandColor(), workspace.getLogoUrl());
    }
}
