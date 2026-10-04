package org.code.api.controllers;

import jakarta.validation.Valid;
import java.util.UUID;
import org.code.api.dto.user.ChangePasswordRequest;
import org.code.api.dto.user.CreatePartnerRequest;
import org.code.api.dto.user.ResetPasswordRequest;
import org.code.api.dto.user.UpdatePartnerRequest;
import org.code.api.dto.user.UpdateProfileRequest;
import org.code.api.dto.user.UserResponse;
import org.code.api.services.AccountLifecycleService;
import org.code.api.services.PartnerProvisioningService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Global administrator-only account provisioning; it does not grant self-service role changes.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
  private final PartnerProvisioningService partners;
  private final AccountLifecycleService accounts;

  /**
   * @param partners administrator provisioning use case
   */
  public UserController(PartnerProvisioningService partners, AccountLifecycleService accounts) {
    this.partners = partners;
    this.accounts = accounts;
  }

  /**
   * Creates a partner without issuing a token on their behalf.
   *
   * @param request validated partner attributes
   * @return created partner metadata
   */
  @PostMapping(produces = "application/json")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "201",
      description = "Partner created")
  public ResponseEntity<UserResponse> create(@Valid @RequestBody CreatePartnerRequest request) {
    return ResponseEntity.status(201).body(partners.create(request));
  }

  /** Returns the current account before organization selection. */
  @GetMapping("/me")
  public UserResponse me() {
    return accounts.me();
  }

  /** Updates only the authenticated user's display name and login address. */
  @PutMapping("/me")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Update my profile",
      description =
          "Only fullName and email are writable; platform and organization roles are unchanged. Email conflicts return 409.")
  public UserResponse updateMe(@Valid @RequestBody UpdateProfileRequest request) {
    return accounts.updateMe(request);
  }

  /** Changes the current password and revokes all existing tokens; sign in again afterward. */
  @PutMapping("/me/password")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Change my password",
      description =
          "Requires the current password. Incorrect current password returns 403. Success invalidates all existing tokens and clears mandatory change state.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "204",
      description = "Password changed; authenticate again")
  public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
    accounts.changePassword(request);
    return ResponseEntity.noContent().build();
  }

  /** Resets a partner's credentials and restricts subsequent sessions until password change. */
  @PutMapping("/{id}/password")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Reset partner password",
      description =
          "ADMINISTRATOR only. Revokes existing tokens and requires a different password before business access. No credential is returned.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "204",
      description = "Temporary credential set; mandatory change required")
  public ResponseEntity<Void> resetPassword(
      @PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
    accounts.resetPassword(id, request);
    return ResponseEntity.noContent().build();
  }

  /** Lists active accounts for platform administrators. */
  @GetMapping
  public Page<UserResponse> list(
      @org.springframework.data.web.PageableDefault(size = 20, sort = "fullName") Pageable page) {
    return accounts.list(page);
  }

  /** Returns a safe account representation for administrators. */
  @GetMapping("/{id}")
  public UserResponse get(@PathVariable UUID id) {
    return accounts.get(id);
  }

  /** Replaces partner metadata without granting administrator privileges. */
  @PutMapping("/{id}")
  public UserResponse update(
      @PathVariable UUID id, @Valid @RequestBody UpdatePartnerRequest request) {
    return accounts.update(id, request);
  }

  /** Deactivates a partner and invalidates its access on subsequent requests. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    accounts.deactivate(id);
    return ResponseEntity.noContent().build();
  }
}
