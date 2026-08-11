package org.code.api.dto.pressing.response;

import org.code.api.domain.enums.DestinationType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PressedBaleResponseDTO(
    UUID id,
    UUID pressingId,
    UUID sortedItemId,
    UUID materialSubtypeId,
    BigDecimal weightKg,
    BigDecimal initialVolumeM3,
    BigDecimal finalVolumeM3,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    DestinationType destinationType,
    UUID destinationId
) {}
