package org.code.api.dto.collection.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Collection intake fields; totals must match input lines and all references share one organization.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
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
    @jakarta.validation.constraints.NotEmpty
    @jakarta.validation.constraints.Size(max=200)
    @Valid
    List<InputItemRequestDTO> inputItems,
    @jakarta.validation.constraints.Size(max=2000) String routeDescription,
    OffsetDateTime departureAt,
    OffsetDateTime arrivalAt,
    @jakarta.validation.constraints.PositiveOrZero @jakarta.validation.constraints.Digits(integer=9,fraction=3) BigDecimal distanceKm
) {
    /** Requires a complete chronological schedule when either timestamp is supplied. */
    @jakarta.validation.constraints.AssertTrue(message="Departure and arrival must be supplied together in chronological order")
    public boolean isScheduleValid() { return departureAt==null&&arrivalAt==null || departureAt!=null&&arrivalAt!=null&&!arrivalAt.isBefore(departureAt); }
}
