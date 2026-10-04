package org.code.api.services;

import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serializes global account administration and rechecks authority after competing mutations. Call
 * inside the caller's transaction, before locking individual account rows.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Component
@lombok.RequiredArgsConstructor
public class GlobalAdministratorGuard {
  private final JdbcTemplate jdbc;
  private final AuthenticatedUserProvider actor;

  /** Shares the first-administrator lock and rejects actors demoted while waiting. */
  public void lock() {
    jdbc.execute("SELECT pg_advisory_xact_lock(724991603)");
    if (jdbc.queryForObject(
            "SELECT count(*) FROM users WHERE id=? AND is_active AND user_role='ADMINISTRATOR'",
            Long.class,
            actor.getCurrentUserId())
        != 1)
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Global administrator access is required");
  }

  /** Preserves at least one active administrator under the global transaction lock. */
  public void requireSuccessor() {
    if (jdbc.queryForObject(
            "SELECT count(*) FROM users WHERE is_active AND user_role='ADMINISTRATOR'", Long.class)
        <= 1)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "The last active global administrator cannot be removed");
  }
}
