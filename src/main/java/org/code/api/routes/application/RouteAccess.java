package org.code.api.routes.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.domain.ports.OrganizationScope;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reuses established effective organization authority and centralizes route command preconditions.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Component
@RequiredArgsConstructor
public class RouteAccess {
  private final OrganizationScope scope;
  private final AuthenticatedUserProvider actor;

  /** Resolves the active selected organization for a permitted scoped read or operational write. */
  public UUID organization() {
    return scope.organizationId();
  }

  /**
   * Requires existing local management or global authority, retaining the organization write lock.
   */
  public UUID manager() {
    UUID organization = scope.organizationId();
    if (!scope.manager())
      throw new AccessDeniedException("Organization manager permission is required");
    return organization;
  }

  /** Shared territorial reference mutation never grants local managers global permissions. */
  public void global() {
    scope.organizationId();
    if (!actor.getCurrentUserRoles().contains(UserRole.ADMINISTRATOR))
      throw new AccessDeniedException("Global administrator permission is required");
  }

  /** Returns the server-resolved actor for transactionally recorded metadata. */
  public UUID actor() {
    return actor.getCurrentUserId();
  }

  /** Rejects missing/negative versions and stale commands before any effect commits. */
  public static void version(Long expected, long actual) {
    if (expected == null || expected < 0)
      throw invalid("A nonnegative current version is required");
    if (expected != actual) throw conflict("The record changed; reload its current version");
  }

  /** Returns a safe missing-resource rejection without disclosing foreign scope. */
  public static ResponseStatusException missing(String resource) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found");
  }

  /** Returns a documented input rejection without database diagnostics. */
  public static ResponseStatusException invalid(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  /** Returns a documented lifecycle/version/history conflict. */
  public static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
