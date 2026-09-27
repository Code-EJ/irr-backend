package org.code.api.dto.sale.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Typed Buyer Response API value.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record BuyerResponseDTO(
    UUID id,
    String name,
    String document,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
