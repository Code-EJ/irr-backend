package org.code.api.dto.donor.response;

import org.code.api.domain.enums.DonorType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DonorResponseDTO(
    UUID id,
    String name,
    String document,
    DonorType donorType,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
