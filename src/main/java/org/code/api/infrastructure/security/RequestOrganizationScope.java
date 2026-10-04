package org.code.api.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.code.api.domain.ports.OrganizationScope;
import org.code.api.organizations.application.OrganizationService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies explicit organization selection and global or organization-local authority per use case.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Component("organizationScope")
public class RequestOrganizationScope implements OrganizationScope {
  private final HttpServletRequest request;
  private final OrganizationService organizations;
  private final JdbcTemplate jdbc;

  /**
   * @param request current servlet request proxy
   * @param organizations current membership application boundary
   */
  public RequestOrganizationScope(
      HttpServletRequest request, OrganizationService organizations, JdbcTemplate jdbc) {
    this.request = request;
    this.organizations = organizations;
    this.jdbc = jdbc;
  }

  private UUID requested() {
    String value = request.getHeader("X-Organization-Id");
    if (value == null || value.isBlank())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "X-Organization-Id is required for business operations");
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Organization-Id must be a UUID");
    }
  }

  /** {@inheritDoc} */
  public UUID organizationId() {
    UUID id = requested();
    lockWritingScope(id);
    return organizations.requireAccess(id);
  }

  /** {@inheritDoc} */
  public boolean manager() {
    UUID id = requested();
    lockWritingScope(id);
    return organizations.canManage(id);
  }

  private void lockWritingScope(UUID id) {
    if (TransactionSynchronizationManager.isActualTransactionActive()
        && !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
      jdbc.query(
          "SELECT id FROM organization WHERE id=? FOR UPDATE", (rs, row) -> rs.getObject(1), id);
    }
  }
}
