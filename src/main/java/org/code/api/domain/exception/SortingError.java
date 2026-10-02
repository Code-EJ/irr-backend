package org.code.api.domain.exception;

import java.util.UUID;
import lombok.Getter;

/**
 * Groups domain failures for sorting operations and HTTP error translation.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public class SortingError extends RuntimeException {

  public SortingError(String message) {
    super(message);
  }

  /** Conceals absent or foreign records behind one resource-not-found failure. */
  @Getter
  public static class NotFound extends SortingError {
    private final UUID sortingId;

    public NotFound(UUID sortingId) {
      super(String.format("Sorting record not found with ID: %s", sortingId));
      this.sortingId = sortingId;
    }
  }

  /** Conceals a missing or foreign raw intake source. */
  @Getter
  public static class InputItemNotFound extends SortingError {
    private final UUID inputItemId;

    public InputItemNotFound(UUID inputItemId) {
      super(String.format("Input item not found with ID: %s", inputItemId));
      this.inputItemId = inputItemId;
    }
  }
}
