package org.code.api.dto.collection.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InputItemResponseDTO(
    UUID id,
    UUID collectionId,
    UUID donationId,
    UUID materialSubtypeId,
    BigDecimal weightKg,
    BigDecimal volumeM3,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
