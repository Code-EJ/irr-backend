package org.code.api.dto.pressing.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.code.api.domain.enums.DestinationType;

/**
 * Typed Pressed Bale Response API value.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record PressedBaleResponseDTO(
    UUID id,
    UUID pressingId,
    UUID sortedItemId,
    UUID materialSubtypeId,
    BigDecimal weightKg,
    BigDecimal initialVolumeM3,
    BigDecimal finalVolumeM3,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    DestinationType destinationType,
    UUID destinationId) {}
