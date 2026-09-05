package org.code.api.dto.sorting.response;

import org.code.api.domain.enums.DestinationType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SortedItemResponseDTO(
    UUID id,
    UUID sortingId,
    UUID inputItemId,
    UUID materialSubtypeId,
    BigDecimal weightKg,
    BigDecimal volumeM3,
    BigDecimal rejectWeightKg,
    BigDecimal rejectVolumeM3,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    DestinationType destinationType,
    UUID destinationId
) {}
