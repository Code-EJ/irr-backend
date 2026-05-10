package org.code.api.dto.donation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.code.api.dto.collection.request.InputItemRequestDTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DonationCreateRequestDTO(
    OffsetDateTime donationDate,
    @NotNull(message = "Total weight is required")
    @Positive(message = "Total weight must be positive")
    BigDecimal totalWeightKg,
    @NotNull(message = "Donor ID is required")
    UUID donorId,
    UUID proofAttachmentId,
    @Valid
    List<InputItemRequestDTO> inputItems
) {}
