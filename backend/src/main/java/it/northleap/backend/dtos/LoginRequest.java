package it.northleap.backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Email required")
    @Email(message = "Email format not valid")
    String email,

    @NotBlank(message = "Password required")
    String password
) {
}
