package org.code.api.domain.exception;

import java.util.UUID;
import lombok.Getter;

/**
 * Donor Error boundary for the IRR application.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */

// Domain failures for organization-owned donors.

public class DonorError extends RuntimeException {

  public DonorError(String message) {
    super(message);
  }

  @Getter
  public static class NotFound extends DonorError {
    private final UUID donorId;

    public NotFound(UUID donorId) {
      super("Donor not found");
      this.donorId = donorId;
    }
  }

  @Getter
  public static class DocumentAlreadyExists extends DonorError {
    private final String document;

    public DocumentAlreadyExists(String document) {
      super("Donor document already exists");
      this.document = document;
    }
  }

  public static class InactiveDonor extends DonorError {
    public InactiveDonor(UUID id) {
      super(String.format("Donor with ID %s is inactive and cannot be modified.", id));
    }
  }
}
