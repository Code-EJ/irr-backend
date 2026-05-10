package org.code.api.dto.team.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamMemberCreateRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    String name,
    @Size(max = 50, message = "Role must be at most 50 characters")
    String role
) {}
