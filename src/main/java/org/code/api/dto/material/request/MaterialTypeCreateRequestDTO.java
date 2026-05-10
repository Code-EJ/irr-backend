package org.code.api.dto.material.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MaterialTypeCreateRequestDTO(
    @NotNull(message = "Category ID is required")
    UUID categoryId,
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    String name
) {}
