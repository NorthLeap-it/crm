package it.northleap.backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OnboardingRequest(
    @NotBlank(message = "Workspace name required")
    String workspaceName,

    @NotBlank(message = "Name required")
    String name,

    @NotBlank(message = "Email required")
    @Email(message = "Email format not valid")
    String email,

    // stesso minimo di AcceptInviteDto - l'account owner (il piu' privilegiato del sistema)
    // non deve poter nascere con una password piu' debole di un invito qualsiasi
    @NotBlank(message = "Password required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password
) {
}
