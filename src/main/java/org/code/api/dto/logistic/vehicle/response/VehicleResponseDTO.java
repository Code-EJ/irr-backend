package org.code.api.dto.logistic.vehicle.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Typed Vehicle Response API value.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record VehicleResponseDTO(
    UUID id,
    String licensePlate,
    String model,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String creatorId) {}
