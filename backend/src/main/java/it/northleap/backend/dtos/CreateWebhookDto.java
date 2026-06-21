package it.northleap.backend.dtos;

import it.northleap.backend.entities.WebhookDirection;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateWebhookDto {
    @NotNull
    private WebhookDirection direction;

    @NotNull
    private String name;

    private String url;

    private List<String> events;
}
