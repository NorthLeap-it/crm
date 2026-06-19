package it.northleap.backend.dtos;

import java.util.UUID;

public record AuthResponse(
     String accessToken,
     String refreshToken,
     UUID userId,
     String email,
     String name
) {

}
