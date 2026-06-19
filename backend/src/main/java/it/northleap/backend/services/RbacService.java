package it.northleap.backend.services;

import it.northleap.backend.entities.PermScope;
import it.northleap.backend.entities.Permission;
import it.northleap.backend.repositories.PermissionRepository;
import it.northleap.backend.security.PermAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final PermissionRepository permissionRepository;

    public record Resolution(boolean allowed, PermScope scope) {
        private static final Resolution DENIED = new Resolution(false, null);
    }

    // se più ruoli hanno permesso sulla stessa risorsa/azione, vince lo scope più ampio (ALL > TEAM > OWN)
    public Resolution resolve(List<UUID> roleIds, String resource, PermAction action) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Resolution.DENIED;
        }

        List<Permission> permissions = permissionRepository.findByRole_IdInAndResource(roleIds, resource);

        return permissions.stream()
                .filter(permission -> isActionAllowed(permission, action))
                .map(Permission::getScope)
                .max(Comparator.comparingInt(this::scopeWeight))
                .map(scope -> new Resolution(true, scope))
                .orElse(Resolution.DENIED);
    }

    private boolean isActionAllowed(Permission permission, PermAction action) {
        return switch (action) {
            case READ -> permission.isCanRead();
            case WRITE -> permission.isCanWrite();
            case EXECUTE -> permission.isCanExecute();
        };
    }

    private int scopeWeight(PermScope scope) {
        return switch (scope) {
            case OWN -> 0;
            case TEAM -> 1;
            case ALL -> 2;
        };
    }
}
