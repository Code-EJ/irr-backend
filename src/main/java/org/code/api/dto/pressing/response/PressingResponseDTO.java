package org.code.api.dto.pressing.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PressingResponseDTO(
    UUID id,
    OffsetDateTime pressingDate,
    Boolean isActive,
    List<PressedBaleResponseDTO> pressedBales,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
