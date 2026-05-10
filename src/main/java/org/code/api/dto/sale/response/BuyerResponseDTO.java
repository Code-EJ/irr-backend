package org.code.api.dto.sale.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BuyerResponseDTO(
    UUID id,
    String name,
    String document,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
