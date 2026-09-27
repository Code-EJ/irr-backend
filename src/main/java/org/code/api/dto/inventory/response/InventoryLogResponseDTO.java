package org.code.api.dto.inventory.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.code.api.domain.enums.OperationType;

/**
 * Validated inventory log response contract fields for the organization-scoped API.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record InventoryLogResponseDTO(
    UUID id,
    UUID materialSubtypeId,
    BigDecimal quantityKg,
    BigDecimal quantityM3,
    OperationType operationType,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
