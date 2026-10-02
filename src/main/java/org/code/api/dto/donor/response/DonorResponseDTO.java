package org.code.api.dto.donor.response;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.code.api.domain.enums.DonorType;
import org.code.api.dto.donor.PostalAddressDTO;

/**
 * Typed Donor Response API value.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record DonorResponseDTO(
    UUID id,
    String name,
    String document,
    DonorType donorType,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    @jakarta.validation.Valid PostalAddressDTO address) {}
