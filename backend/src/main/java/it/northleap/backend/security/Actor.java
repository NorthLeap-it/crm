package it.northleap.backend.security;

import java.util.List;
import java.util.UUID;

// "Chi sta facendo la richiesta": utente autenticato via JWT, oppure (Fase 6, non ancora
// implementato) un'integrazione autenticata via header X-Api-Key. Parallelo a UserDetails/
// UserPrincipal ma con scopo diverso: UserDetails serve a Spring Security per il SE è autenticato,
// Actor serve alla logica RBAC custom per il COSA può fare. Tenuti volutamente separati.
public record Actor(
        UUID id,
        ActorType type,
        String email,
        List<UUID> roleIds
) {
    public static final String REQUEST_ATTRIBUTE = "actor";
}
