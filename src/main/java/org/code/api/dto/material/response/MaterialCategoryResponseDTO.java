package org.code.api.dto.material.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Typed Material Category Response API value.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record MaterialCategoryResponseDTO(
    UUID id,
    String name,
    Boolean isActive,
    Long version,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
