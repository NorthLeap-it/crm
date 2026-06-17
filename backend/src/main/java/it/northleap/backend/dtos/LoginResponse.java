package it.northleap.backend.dtos;

import java.util.UUID;

public record LoginResponse(
     String accessToken,
     UUID userId,
     String email,
     String name
) {

}

