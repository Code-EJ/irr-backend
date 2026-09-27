package org.code.api.dto.donation.response;

import org.code.api.dto.collection.response.InputItemResponseDTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Typed Donation Response  API value.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */

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
