package org.code.api.dto.collection.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CollectionCreateRequestDTO(
    @NotNull(message = "Realization date is required")
    OffsetDateTime realizationDate,
    @NotNull(message = "Total weight is required")
    @Positive(message = "Total weight must be positive")
    BigDecimal totalWeightKg,
    @NotNull(message = "Vehicle ID is required")
    UUID vehicleId,
    @NotNull(message = "Driver ID is required")
    UUID driverId,
    UUID mtrGeneratorId,
    UUID mtrDestinatorId,
    UUID collectionDiaryId,
    Set<UUID> teamMemberIds,
    @Valid
    List<InputItemRequestDTO> inputItems
) {}
