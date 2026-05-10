package org.code.api.dto.sorting.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para um item triado com peso/volume aproveitado e rejeito.
 */
public record SortedItemRequestDTO(
    UUID inputItemId,
    @NotNull(message = "Material subtype ID is required")
    UUID materialSubtypeId,
    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    BigDecimal weightKg,
    @NotNull(message = "Volume is required")
    @Positive(message = "Volume must be positive")
    BigDecimal volumeM3,
    @PositiveOrZero(message = "Reject weight must be zero or positive")
    BigDecimal rejectWeightKg,
    @PositiveOrZero(message = "Reject volume must be zero or positive")
    BigDecimal rejectVolumeM3
) {}
