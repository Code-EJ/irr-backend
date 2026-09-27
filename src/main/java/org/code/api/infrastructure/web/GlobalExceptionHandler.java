package org.code.api.infrastructure.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.code.api.domain.exception.AuthError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global Exception Handler boundary for the IRR application.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(AuthError.Unauthorized.class)
  public ResponseEntity<Object> handleUnauthorized(AuthError.Unauthorized ex) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", HttpStatus.UNAUTHORIZED.value());
    body.put("error", "Unauthorized");
    body.put("message", ex.getMessage());

    return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
  }

  /**
   * @return a safe status response for rejected request scope
   */
  @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
  public ResponseEntity<?> requestRejected(
      org.springframework.web.server.ResponseStatusException error) {
    return ResponseEntity.status(error.getStatusCode())
        .body(
            Map.of(
                "error",
                "request_rejected",
                "message",
                error.getReason() == null ? "Request rejected" : error.getReason()));
  }

  /**
   * @return the same result for missing and inaccessible organizational scope
   */
  @ExceptionHandler(org.code.api.organizations.domain.OrganizationAccessDenied.class)
  public ResponseEntity<?> scopeNotFound(
      org.code.api.organizations.domain.OrganizationAccessDenied error) {
    return ResponseEntity.status(404)
        .body(
            Map.of(
                "error",
                "organization_not_found",
                "message",
                "Organization or active membership not found"));
  }

  /**
   * @return a generic relational conflict without internal database details
   */
  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity<?> integrityConflict(
      org.springframework.dao.DataIntegrityViolationException error) {
    return ResponseEntity.status(409)
        .body(
            Map.of(
                "error",
                "record_conflict",
                "message",
                "The operation conflicts with an existing or referenced record"));
  }
}
