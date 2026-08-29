package org.code.api.dto.donation.request;



import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.code.api.dto.collection.request.InputItemRequestDTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DonationUpdateRequestDTO(
        OffsetDateTime donationDate,
        @NotNull(message = "Total weight is required")
        @Positive(message = "Total weight must be positive")
        BigDecimal totalWeightKg,
        UUID proofAttachmentId,
        @NotEmpty(message = "At least one input item is required")
        @Valid
        List<InputItemRequestDTO> inputItems
) {}