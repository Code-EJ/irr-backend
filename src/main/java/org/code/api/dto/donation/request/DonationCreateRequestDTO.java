package org.code.api.dto.donation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.code.api.dto.collection.request.InputItemRequestDTO;

/**
 * Typed Donation Create Request API value.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record DonationCreateRequestDTO(
    OffsetDateTime donationDate,
    @NotNull(message = "Total weight is required")
        @Positive(message = "Total weight must be positive")
        @jakarta.validation.constraints.Digits(integer = 11, fraction = 4)
        BigDecimal totalWeightKg,
    @NotNull(message = "Donor ID is required") UUID donorId,
    UUID proofAttachmentId,
    @Valid List<InputItemRequestDTO> inputItems) {}
