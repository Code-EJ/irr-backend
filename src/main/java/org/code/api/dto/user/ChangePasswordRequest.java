package org.code.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Write-only credential input. Never include its contents in logs or diagnostics.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record ChangePasswordRequest(
    @NotBlank @Size(max = 72) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
        String currentPassword,
    @NotBlank @Size(min = 8, max = 72) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
        String newPassword) {
  @Override
  public String toString() {
    return "ChangePasswordRequest[REDACTED]";
  }
}
