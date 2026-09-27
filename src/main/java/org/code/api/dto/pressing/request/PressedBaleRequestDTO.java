package org.code.api.dto.pressing.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.code.api.domain.enums.DestinationType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A pressed bale that preserves source mass and reduces occupied volume.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record PressedBaleRequestDTO(
    @NotNull(message = "Source item ID is required")
    UUID sortedItemId,
    @NotNull(message = "Material subtype ID is required")
    UUID materialSubtypeId,
    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    @jakarta.validation.constraints.Digits(integer = 11, fraction = 4)
    BigDecimal weightKg,
    @NotNull(message = "Initial volume is required")
    @Positive(message = "Initial volume must be positive")
    @jakarta.validation.constraints.Digits(integer = 11, fraction = 4)
    BigDecimal initialVolumeM3,
    @NotNull(message = "Final volume is required")
    @Positive(message = "Final volume must be positive")
    @jakarta.validation.constraints.Digits(integer = 11, fraction = 4)
    BigDecimal finalVolumeM3,
    DestinationType destinationType,
    UUID destinationId
) {}
