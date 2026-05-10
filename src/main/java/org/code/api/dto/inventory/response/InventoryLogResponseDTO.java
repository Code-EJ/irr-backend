package org.code.api.dto.inventory.response;

import org.code.api.domain.enums.OperationType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de resposta do Livro Razão (Append-Only). Somente leitura.
 */
public record InventoryLogResponseDTO(
    UUID id,
    UUID materialSubtypeId,
    BigDecimal quantityKg,
    BigDecimal quantityM3,
    OperationType operationType,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
