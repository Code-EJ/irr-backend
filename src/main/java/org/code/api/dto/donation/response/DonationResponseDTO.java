package org.code.api.dto.donation.response;

import org.code.api.dto.collection.response.InputItemResponseDTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DonationResponseDTO(
    UUID id,
    OffsetDateTime donationDate,
    BigDecimal totalWeightKg,
    UUID donorId,
    UUID proofAttachmentId,
    Boolean isActive,
    List<InputItemResponseDTO> inputItems,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
