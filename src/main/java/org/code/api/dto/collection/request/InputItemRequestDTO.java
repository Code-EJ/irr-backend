package org.code.api.dto.collection.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para um item de entrada (InputItem) usado em Collection ou Donation.
 */
public record InputItemRequestDTO(
    @NotNull(message = "Material subtype ID is required")
    UUID materialSubtypeId,
    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    BigDecimal weightKg,
    @NotNull(message = "Volume is required")
    @Positive(message = "Volume must be positive")
    BigDecimal volumeM3
) {}
