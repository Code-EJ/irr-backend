package org.code.api.dto.sorting.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;
import org.code.api.domain.enums.DestinationType;

/**
 * Sorting quantities allocated from one intake item.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record SortedItemRequestDTO(
    @NotNull(message = "Source item ID is required") UUID inputItemId,
    @NotNull(message = "Material subtype ID is required") UUID materialSubtypeId,
    @NotNull(message = "Weight is required")
        @Positive(message = "Weight must be positive")
        @jakarta.validation.constraints.Digits(integer = 11, fraction = 4)
        BigDecimal weightKg,
    @NotNull(message = "Volume is required")
        @Positive(message = "Volume must be positive")
        @jakarta.validation.constraints.Digits(integer = 11, fraction = 4)
        BigDecimal volumeM3,
    @PositiveOrZero(message = "Reject weight must be zero or positive")
        @jakarta.validation.constraints.Digits(integer = 11, fraction = 4)
        BigDecimal rejectWeightKg,
    @PositiveOrZero(message = "Reject volume must be zero or positive")
        @jakarta.validation.constraints.Digits(integer = 11, fraction = 4)
        BigDecimal rejectVolumeM3,
    DestinationType destinationType,
    UUID destinationId) {
  @AssertTrue(message = "rejectWeightKg must not exceed weightKg")
  public boolean isRejectWeightValid() {
    return rejectWeightKg == null || weightKg == null || rejectWeightKg.compareTo(weightKg) <= 0;
  }

  @AssertTrue(message = "rejectVolumeM3 must not exceed volumeM3")
  public boolean isRejectVolumeValid() {
    return rejectVolumeM3 == null || volumeM3 == null || rejectVolumeM3.compareTo(volumeM3) <= 0;
  }
}
