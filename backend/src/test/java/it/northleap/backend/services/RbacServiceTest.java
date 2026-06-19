package it.northleap.backend.services;

import it.northleap.backend.entities.PermScope;
import it.northleap.backend.entities.Permission;
import it.northleap.backend.entities.Role;
import it.northleap.backend.repositories.PermissionRepository;
import it.northleap.backend.security.PermAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    private RbacService rbacService;

    private UUID roleOwnId;
    private UUID roleAllId;

    @BeforeEach
    void setUp() {
        rbacService = new RbacService(permissionRepository);
        roleOwnId = UUID.randomUUID();
        roleAllId = UUID.randomUUID();
    }

    private Permission permission(UUID roleId, String resource, boolean read, boolean write, boolean execute, PermScope scope) {
        Role role = new Role();
        role.setId(roleId);
        Permission permission = new Permission();
        permission.setRole(role);
        permission.setResource(resource);
        permission.setCanRead(read);
        permission.setCanWrite(write);
        permission.setCanExecute(execute);
        permission.setScope(scope);
        return permission;
    }

    @Test
    void deniesWhenNoRoleIdsGiven() {
        RbacService.Resolution resolution = rbacService.resolve(List.of(), "contact", PermAction.READ);

        assertFalse(resolution.allowed());
    }

    @Test
    void deniesWhenNoMatchingPermissionRow() {
        when(permissionRepository.findByRole_IdInAndResource(any(), eq("contact"))).thenReturn(List.of());

        RbacService.Resolution resolution = rbacService.resolve(List.of(roleOwnId), "contact", PermAction.READ);

        assertFalse(resolution.allowed());
    }

    @Test
    void allowsWithScopeOwnForSingleMatchingRole() {
        when(permissionRepository.findByRole_IdInAndResource(any(), eq("contact")))
                .thenReturn(List.of(permission(roleOwnId, "contact", false, true, false, PermScope.OWN)));

        RbacService.Resolution resolution = rbacService.resolve(List.of(roleOwnId), "contact", PermAction.WRITE);

        assertTrue(resolution.allowed());
        assertEquals(PermScope.OWN, resolution.scope());
    }

    @Test
    void widestScopeWinsWhenMultipleRolesGrantAccess() {
        when(permissionRepository.findByRole_IdInAndResource(any(), eq("contact")))
                .thenReturn(List.of(
                        permission(roleOwnId, "contact", true, false, false, PermScope.OWN),
                        permission(roleAllId, "contact", true, false, false, PermScope.ALL)
                ));

        RbacService.Resolution resolution = rbacService.resolve(List.of(roleOwnId, roleAllId), "contact", PermAction.READ);

        assertTrue(resolution.allowed());
        assertEquals(PermScope.ALL, resolution.scope());
    }

    @Test
    void deniesActionWhenPermissionRowExistsButFlagIsFalse() {
        when(permissionRepository.findByRole_IdInAndResource(any(), eq("workflow")))
                .thenReturn(List.of(permission(roleOwnId, "workflow", true, true, false, PermScope.ALL)));

        RbacService.Resolution resolution = rbacService.resolve(List.of(roleOwnId), "workflow", PermAction.EXECUTE);

        assertFalse(resolution.allowed());
    }
}
