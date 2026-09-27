package org.code.api.dto.collection.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Typed Collection Response API value.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record CollectionResponseDTO(
    UUID id,
    OffsetDateTime realizationDate,
    BigDecimal totalWeightKg,
    UUID vehicleId,
    UUID driverId,
    UUID mtrGeneratorId,
    UUID mtrDestinatorId,
    UUID collectionDiaryId,
    Boolean isActive,
    Set<UUID> teamMemberIds,
    List<InputItemResponseDTO> inputItems,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String routeDescription,
    OffsetDateTime departureAt,
    OffsetDateTime arrivalAt,
    BigDecimal distanceKm) {}
