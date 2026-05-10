package org.code.api.dto.sale.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SaleResponseDTO(
    UUID id,
    OffsetDateTime saleDate,
    UUID buyerId,
    UUID nfeAttachmentId,
    UUID mtrAttachmentId,
    UUID cdfAttachmentId,
    BigDecimal totalValue,
    Boolean isActive,
    List<SaleItemResponseDTO> saleItems,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
