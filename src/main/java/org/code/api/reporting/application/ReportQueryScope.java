package org.code.api.reporting.application;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.domain.ports.OrganizationScope;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Shared native report scope and Brazilian period validation.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Component
@RequiredArgsConstructor
public class ReportQueryScope {
  public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
  private final OrganizationScope scope;
  private final NamedParameterJdbcTemplate jdbc;

  /** Resolves bounded historical dimensions without exposing foreign organization facts. */
  public MapSqlParameterSource parameters(
      LocalDate from, LocalDate to, UUID municipality, UUID route, UUID vehicle) {
    UUID org = scope.organizationId();
    if (from == null
        || to == null
        || to.isBefore(from)
        || ChronoUnit.DAYS.between(from, to) > 365
        || from.getYear() < 1
        || to.getYear() > 9998)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Supply an ordered period of at most 366 calendar days");
    var p =
        new MapSqlParameterSource()
            .addValue("org", org)
            .addValue("municipality", municipality)
            .addValue("route", route)
            .addValue("vehicle", vehicle)
            .addValue("start", from.atStartOfDay(ZONE).toOffsetDateTime())
            .addValue("end", to.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime());
    if (municipality != null)
      require("SELECT count(*) FROM municipality WHERE id=:municipality", p);
    if (route != null)
      require("SELECT count(*) FROM route WHERE id=:route AND organization_id=:org", p);
    if (vehicle != null)
      require("SELECT count(*) FROM vehicle WHERE id=:vehicle AND organization_id=:org", p);
    if (route != null
        && municipality != null
        && jdbc.queryForObject(
                "SELECT count(*) FROM route WHERE id=:route AND municipality_id=:municipality",
                p,
                Long.class)
            == 0)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Route and municipality filters do not match");
    return p;
  }

  /** Rejects absent or foreign historical dimensions under the non-disclosure policy. */
  private void require(String query, MapSqlParameterSource p) {
    if (jdbc.queryForObject(query, p, Long.class) == 0)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report filter not found");
  }
}
