package org.code.api.dto.pressing.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Typed Pressing Response API value.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record PressingResponseDTO(
    UUID id,
    OffsetDateTime pressingDate,
    Boolean isActive,
    List<PressedBaleResponseDTO> pressedBales,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String status) {}
