package it.northleap.backend.config;

import it.northleap.backend.entities.ObjectType;
import it.northleap.backend.entities.PermScope;
import it.northleap.backend.entities.Permission;
import it.northleap.backend.entities.Role;
import it.northleap.backend.repositories.ObjectTypeRepository;
import it.northleap.backend.repositories.PermissionRepository;
import it.northleap.backend.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

// Seed dei permessi di default per i ruoli di sistema. A differenza della prima versione (Fase 2,
// che seminava solo le 5 risorse di sistema fisse e si fermava se la tabella non era vuota),
// questo seeder fa upsert per-(role,resource): inserisce solo le combinazioni mancanti, senza
// toccare quelle già presenti. Questo è necessario perché ora le risorse includono anche le key
// degli ObjectType seminati da ObjectTypeSeeder, che possono crescere nel tempo — stessa logica
// dello script seed.ts originale (upsert per coppia, non "salta se la tabella non è vuota").
// Gira dopo ObjectTypeSeeder (vedi @Order) per vedere tutte le ObjectType key correnti.
@Component
@RequiredArgsConstructor
@Order(3)
public class PermissionSeeder implements ApplicationRunner {

    private static final List<String> FIXED_RESOURCES = List.of("page", "chart", "workflow", "user", "apikey");

    private final RoleRepository roleRepository;
    private final ObjectTypeRepository objectTypeRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<String> objectTypeKeys = objectTypeRepository.findAll().stream().map(ObjectType::getKey).toList();
        List<String> resources = Stream.concat(FIXED_RESOURCES.stream(), objectTypeKeys.stream()).toList();

        List<Permission> toCreate = new ArrayList<>();
        for (Role role : roleRepository.findAll()) {
            for (String resource : resources) {
                if (permissionRepository.findByRole_IdAndResource(role.getId(), resource).isEmpty()) {
                    toCreate.add(buildPermission(role, resource));
                }
            }
        }
        if (!toCreate.isEmpty()) {
            permissionRepository.saveAll(toCreate);
        }
    }

    private Permission buildPermission(Role role, String resource) {
        Permission permission = new Permission();
        permission.setRole(role);
        permission.setResource(resource);
        permission.setScope(PermScope.ALL);

        switch (role.getKey()) {
            case "owner", "admin", "manager" -> {
                permission.setCanRead(true);
                permission.setCanWrite(true);
                permission.setCanExecute(true);
            }
            case "agent" -> {
                permission.setCanRead(true);
                permission.setCanWrite(true);
            }
            case "viewer" -> permission.setCanRead(true);
            default -> {
                // ruoli custom futuri: nessun permesso di default
            }
        }

        return permission;
    }
}
