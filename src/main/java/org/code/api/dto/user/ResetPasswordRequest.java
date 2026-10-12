package org.code.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Administrator-supplied temporary password, never serialized in responses or diagnostics.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record ResetPasswordRequest(
    @NotBlank @Size(min = 8, max = 72) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
        String temporaryPassword) {
  @Override
  public String toString() {
    return "ResetPasswordRequest[REDACTED]";
  }
}
