package org.code.api.dto.material.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MaterialSubtypeUpdateRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    String name,
    @NotNull(message = "Version is required for optimistic locking")
    Long version
) {}
