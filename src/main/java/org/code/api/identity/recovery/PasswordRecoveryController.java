package org.code.api.identity.recovery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public recovery operations with exact versioned paths and safe request diagnostics.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequestMapping("/api/v1/session/password-recovery")
@lombok.RequiredArgsConstructor
public class PasswordRecoveryController {
  private final PasswordRecoveryService recovery;

  /** Returns an identical neutral result for existing, absent, inactive and throttled addresses. */
  @PostMapping("/request")
  @Operation(
      summary = "Request a password recovery link",
      description =
          "Public. Returns 202 without revealing account existence or rate-limit state. Disabled/unavailable infrastructure returns 503 for all accounts.")
  @ApiResponse(responseCode = "202", description = "Request accepted without account disclosure")
  public ResponseEntity<Void> request(
      @Valid @RequestBody RecoveryRequest input, HttpServletRequest request) {
    recovery.request(input.email(), request.getRemoteAddr());
    return ResponseEntity.accepted().build();
  }

  /** Replaces credentials once, revokes prior sessions and clears the mandatory change state. */
  @PostMapping("/confirm")
  @Operation(
      summary = "Confirm password recovery",
      description =
          "Public. Valid one-use link returns 204. Invalid, expired, used or revoked links return 400. New credentials follow the BCrypt UTF-8 policy. Previous sessions are revoked.")
  @ApiResponse(responseCode = "204", description = "Password changed and recovery token consumed")
  public ResponseEntity<Void> confirm(
      @Valid @RequestBody RecoveryConfirmation input, HttpServletRequest request) {
    recovery.confirm(input.token(), input.newPassword(), request.getRemoteAddr());
    return ResponseEntity.noContent().build();
  }

  /** Bounded recipient input; never renders the address in diagnostic strings. */
  public record RecoveryRequest(@NotBlank @Email @Size(max = 255) String email) {
    @Override
    public String toString() {
      return "RecoveryRequest[redacted]";
    }
  }

  /** Redacted credential input; URL-safe token syntax is validated before lookup. */
  public record RecoveryConfirmation(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
      @NotBlank @Size(min = 8, max = 72) String newPassword) {
    @Override
    public String toString() {
      return "RecoveryConfirmation[redacted]";
    }
  }
}
