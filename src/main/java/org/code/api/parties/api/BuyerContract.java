package org.code.api.parties.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Explicit HTTP contracts for organization-owned buyers.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public final class BuyerContract {
  private BuyerContract() {}

  /** Validated replacement fields; lifecycle changes use DELETE. */
  @io.swagger.v3.oas.annotations.media.Schema(name = "BuyerRequest")
  public record Request(
      @NotBlank @Size(max = 255) String name,
      @Pattern(
              regexp = "(?:[0-9]{11}|[0-9]{14})?",
              message = "Document must contain 11 or 14 digits")
          String document) {}

  /** Safe response without persistence entities or account data. */
  @io.swagger.v3.oas.annotations.media.Schema(name = "BuyerResponse")
  public record Response(
      UUID id,
      String name,
      String document,
      boolean isActive,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}
}
