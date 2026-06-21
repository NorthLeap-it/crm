package it.northleap.backend.dtos;

// la chiave in chiaro è restituita UNA SOLA VOLTA, alla creazione - da quel momento esiste solo
// il suo hash nel DB, non è recuperabile (stesso vincolo di sicurezza dell'originale)
public record ApiKeyCreatedResponse(String apiKey) {
}
