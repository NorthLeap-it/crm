package it.northleap.backend.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AcceptInviteDto {
    @NotNull
    private String token;

    @NotNull
    private String name;

    @NotNull
    @Size(min = 8)
    private String password;
}
