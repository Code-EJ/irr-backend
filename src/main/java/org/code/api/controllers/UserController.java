package org.code.api.controllers;

import jakarta.validation.Valid;
import org.code.api.dto.user.CreatePartnerRequest;
import org.code.api.dto.user.UserResponse;
import org.code.api.services.PartnerProvisioningService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Administrator-only partner provisioning; it does not grant self-service role changes.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
  private final PartnerProvisioningService partners;
  private final org.code.api.services.AccountLifecycleService accounts;

  /**
   * @param partners administrator provisioning use case
   */
  public UserController(
      PartnerProvisioningService partners, org.code.api.services.AccountLifecycleService accounts) {
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

  /** Lists active accounts for platform administrators. */
  @GetMapping
  public org.springframework.data.domain.Page<UserResponse> list(
      @org.springframework.data.web.PageableDefault(size = 20, sort = "fullName")
          org.springframework.data.domain.Pageable page) {
    return accounts.list(page);
  }

  /** Returns a safe account representation for administrators. */
  @GetMapping("/{id}")
  public UserResponse get(@PathVariable java.util.UUID id) {
    return accounts.get(id);
  }

  /** Replaces partner metadata without granting administrator privileges. */
  @PutMapping("/{id}")
  public UserResponse update(
      @PathVariable java.util.UUID id,
      @Valid @RequestBody org.code.api.dto.user.UpdatePartnerRequest request) {
    return accounts.update(id, request);
  }

  /** Deactivates a partner and invalidates its access on subsequent requests. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable java.util.UUID id) {
    accounts.deactivate(id);
    return ResponseEntity.noContent().build();
  }
}
