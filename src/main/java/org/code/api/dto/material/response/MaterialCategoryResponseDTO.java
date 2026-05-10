package org.code.api.dto.material.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MaterialCategoryResponseDTO(
    UUID id,
    String name,
    Boolean isActive,
    Long version,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
