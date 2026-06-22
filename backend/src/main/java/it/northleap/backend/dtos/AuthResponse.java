package it.northleap.backend.dtos;

import java.util.UUID;

// I token vivono solo nei cookie httpOnly (mai nel body JSON, altrimenti JS potrebbe comunque
// leggerli dalla risposta e annullare il vantaggio di httpOnly) - vedi AuthCookieService.
public record AuthResponse(
     UUID userId,
     String email,
     String name
) {

}
