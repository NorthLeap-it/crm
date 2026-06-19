package it.northleap.backend.dtos;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank(message = "Refresh token required")
    String refreshToken
) {
}
