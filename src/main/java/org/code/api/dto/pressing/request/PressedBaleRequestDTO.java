package org.code.api.dto.pressing.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para um fardo prensado com compactação de volume.
 */
public record PressedBaleRequestDTO(
    UUID sortedItemId,
    @NotNull(message = "Material subtype ID is required")
    UUID materialSubtypeId,
    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    BigDecimal weightKg,
    @NotNull(message = "Initial volume is required")
    @Positive(message = "Initial volume must be positive")
    BigDecimal initialVolumeM3,
    @NotNull(message = "Final volume is required")
    @Positive(message = "Final volume must be positive")
    BigDecimal finalVolumeM3
) {}
