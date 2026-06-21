package it.northleap.backend.dtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// esclude sempre passwordHash, stesso destructuring-omit dell'originale
public record UserSummary(UUID id, String email, String name, String avatarUrl, boolean isActive,
                           List<String> roles, Instant createdAt) {
}
