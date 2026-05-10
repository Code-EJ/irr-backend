package org.code.api.dto.sale.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SaleCreateRequestDTO(
    @NotNull(message = "Sale date is required")
    OffsetDateTime saleDate,
    @NotNull(message = "Buyer ID is required")
    UUID buyerId,
    UUID nfeAttachmentId,
    UUID mtrAttachmentId,
    UUID cdfAttachmentId,
    @NotNull(message = "Total value is required")
    @Positive(message = "Total value must be positive")
    BigDecimal totalValue,
    @Valid
    List<SaleItemRequestDTO> saleItems
) {}
