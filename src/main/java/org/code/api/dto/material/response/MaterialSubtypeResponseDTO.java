package org.code.api.dto.material.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Typed Material Subtype Response API value.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record MaterialSubtypeResponseDTO(
    UUID id,
    UUID typeId,
    String name,
    Boolean isActive,
    Long version,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
