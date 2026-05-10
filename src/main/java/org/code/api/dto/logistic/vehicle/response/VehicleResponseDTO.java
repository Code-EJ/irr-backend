package org.code.api.dto.logistic.vehicle.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleResponseDTO(
    UUID id,
    String licensePlate,
    String model,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String creatorId
) {}
