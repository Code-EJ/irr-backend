package org.code.api.domain.exception;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;

/**
 * Groups domain failures for pressing operations and HTTP error translation.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public class PressingError extends RuntimeException {

  public PressingError(String message) {
    super(message);
  }

  /** Conceals absent or foreign records behind one resource-not-found failure. */
  @Getter
  public static class NotFound extends PressingError {
    private final UUID pressingId;

    public NotFound(UUID pressingId) {
      super(String.format("Pressing record not found with ID: %s", pressingId));
      this.pressingId = pressingId;
    }
  }

  /** Conceals a missing or foreign sorting source. */
  @Getter
  public static class SortedItemNotFound extends PressingError {
    private final UUID sortedItemId;

    public SortedItemNotFound(UUID sortedItemId) {
      super(String.format("Sorted item not found with ID: %s", sortedItemId));
      this.sortedItemId = sortedItemId;
    }
  }

  @Getter
  public static class InvalidCompaction extends PressingError {
    private final BigDecimal initialVolumeM3;
    private final BigDecimal finalVolumeM3;

    public InvalidCompaction(BigDecimal initialVolumeM3, BigDecimal finalVolumeM3) {
      super(
          "Pressing must compact volume: finalVolumeM3 must be less than initialVolumeM3. "
              + "Got initial="
              + initialVolumeM3
              + ", final="
              + finalVolumeM3);
      this.initialVolumeM3 = initialVolumeM3;
      this.finalVolumeM3 = finalVolumeM3;
    }
  }
}
