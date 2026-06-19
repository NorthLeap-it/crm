package it.northleap.backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OnboardingRequest(
    @NotBlank(message = "Workspace name required")
    String workspaceName,

    @NotBlank(message = "Name required")
    String name,

    @NotBlank(message = "Email required")
    @Email(message = "Email format not valid")
    String email,

    @NotBlank(message = "Password required")
    String password
) {
}
