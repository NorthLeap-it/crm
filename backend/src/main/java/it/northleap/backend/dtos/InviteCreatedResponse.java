package it.northleap.backend.dtos;

// inviteToken in chiaro restituito al chiamante (un admin) per costruire il link da inviare via
// email; in produzione l'originale invierebbe l'email lui stesso, non implementato qui (nessun
// provider email configurato per questo flusso - send_email nel workflow engine usa Resend, ma
// è un caso d'uso diverso)
public record InviteCreatedResponse(String inviteToken, String email) {
}
