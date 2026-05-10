package org.code.api.dto.collection.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    OffsetDateTime updatedAt
) {}
