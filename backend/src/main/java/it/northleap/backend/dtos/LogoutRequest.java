package it.northleap.backend.dtos;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
    @NotBlank(message = "Refresh token required")
    String refreshToken
) {
}
