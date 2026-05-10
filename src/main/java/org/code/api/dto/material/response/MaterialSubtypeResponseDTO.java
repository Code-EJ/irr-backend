package org.code.api.dto.material.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MaterialSubtypeResponseDTO(
    UUID id,
    UUID typeId,
    String name,
    Boolean isActive,
    Long version,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
