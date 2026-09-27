package org.code.api.dto.sale.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Typed Sale Item Response API value.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record SaleItemResponseDTO(
    UUID id,
    UUID saleId,
    UUID materialSubtypeId,
    BigDecimal weightKg,
    BigDecimal volumeM3,
    BigDecimal unitPrice,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
