package org.code.api.dto.collection.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Validated input item request contract fields for the organization-scoped API.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record InputItemRequestDTO(
    @NotNull(message = "Material subtype ID is required") UUID materialSubtypeId,
    @NotNull(message = "Weight is required")
        @Positive(message = "Weight must be positive")
        @jakarta.validation.constraints.Digits(integer = 11, fraction = 4)
        BigDecimal weightKg,
    @NotNull(message = "Volume is required")
        @Positive(message = "Volume must be positive")
        @jakarta.validation.constraints.Digits(integer = 11, fraction = 4)
        BigDecimal volumeM3) {}
