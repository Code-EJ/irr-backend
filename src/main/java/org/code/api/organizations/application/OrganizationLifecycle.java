package org.code.api.organizations.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.organizations.domain.Organization;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Administrator-controlled organization metadata and deactivation with transactional audit details.
 * Business records and membership history remain intact after deactivation.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
@lombok.RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class OrganizationLifecycle {
  private final JdbcTemplate jdbc;
  private final AuthenticatedUserProvider actor;
  private final ObjectMapper json;

  /** Replaces descriptive metadata without granting any business access. */
  @Transactional
  public Organization update(UUID id, String name, String type) {
    Organization previous = locked(id);
    jdbc.update(
        "UPDATE organization SET name=?,organization_type=?,updated_at=CURRENT_TIMESTAMP WHERE"
            + " id=?",
        name.strip(),
        type.strip(),
        id);
    audit(
        id,
        "UPDATE",
        Map.of(
            "previousName",
            previous.name(),
            "previousType",
            previous.organizationType(),
            "name",
            name.strip(),
            "organizationType",
            type.strip()));
    return new Organization(id, name.strip(), type.strip(), previous.createdAt());
  }

  /** Deactivates the organization and denies new scoped access after this transaction commits. */
  @Transactional
  public void deactivate(UUID id) {
    locked(id);
    jdbc.update(
        "UPDATE organization SET is_active=false,updated_at=CURRENT_TIMESTAMP WHERE id=?", id);
    audit(id, "DEACTIVATE", Map.of());
  }

  private Organization locked(UUID id) {
    var found =
        jdbc.query(
            "SELECT id,name,organization_type,created_at FROM organization WHERE id=? AND is_active"
                + " FOR UPDATE",
            (r, n) ->
                new Organization(
                    r.getObject("id", UUID.class),
                    r.getString("name"),
                    r.getString("organization_type"),
                    r.getObject("created_at", OffsetDateTime.class)),
            id);
    if (found.isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found");
    return found.getFirst();
  }

  private void audit(UUID id, String action, Map<String, String> details) {
    try {
      jdbc.update(
          "INSERT INTO organization_access_audit(id,organization_id,actor_id,action,details) VALUES"
              + " (?,?,?,?,CAST(? AS jsonb))",
          UUID.randomUUID(),
          id,
          actor.getCurrentUserId(),
          action,
          json.writeValueAsString(details));
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Cannot serialize organization audit details", error);
    }
  }
}
