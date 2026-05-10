package org.code.api.dto.material.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MaterialTypeResponseDTO(
    UUID id,
    UUID categoryId,
    String name,
    Boolean isActive,
    Long version,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
