package org.code.api.reporting.application;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.domain.ports.OrganizationScope;
import org.code.api.reporting.api.OperationalReport;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Aggregates independent facts in one PostgreSQL statement, avoiding join fan-out.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OperationalReportService {
  private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
  private final OrganizationScope scope;
  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Validates effective scope, historical filters and dates before reading a consistent live
   * snapshot.
   */
  @Transactional(readOnly = true)
  public OperationalReport get(
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
    var start = from.atStartOfDay(ZONE).toOffsetDateTime();
    var end = to.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();
    var p =
        new MapSqlParameterSource()
            .addValue("org", org)
            .addValue("municipality", municipality)
            .addValue("route", route)
            .addValue("vehicle", vehicle)
            .addValue("start", start)
            .addValue("end", end);
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
    return jdbc.queryForObject(
        """
 WITH executions AS (
 SELECT distance_km,started_at,finished_at FROM route_execution
 WHERE organization_id=:org AND status='COMPLETED' AND finished_at>=:start AND finished_at<:end
 AND (CAST(:municipality AS uuid) IS NULL OR municipality_id=:municipality)
 AND (CAST(:route AS uuid) IS NULL OR route_id=:route)
 AND (CAST(:vehicle AS uuid) IS NULL OR vehicle_id=:vehicle)
 ), receipts AS (
 SELECT c.total_weight_kg,c.route_execution_id FROM collection c
 LEFT JOIN route_execution e ON e.id=c.route_execution_id AND e.organization_id=c.organization_id
 WHERE c.organization_id=:org AND c.is_active AND c.realization_date>=:start AND c.realization_date<:end
 AND (CAST(:municipality AS uuid) IS NULL OR e.municipality_id=:municipality)
 AND (CAST(:route AS uuid) IS NULL OR e.route_id=:route)
 AND (CAST(:vehicle AS uuid) IS NULL OR c.vehicle_id=:vehicle)
 ), execution_totals AS (
 SELECT count(*) completed,count(distance_km) measured,sum(distance_km) km,
 coalesce(sum(extract(epoch FROM finished_at-started_at)),0) seconds FROM executions
 ), receipt_totals AS (
 SELECT count(*) receipts,coalesce(sum(total_weight_kg),0) kg,
 count(*) FILTER(WHERE route_execution_id IS NULL) unlinked FROM receipts
 ) SELECT * FROM execution_totals CROSS JOIN receipt_totals
 """,
        p,
        (r, n) ->
            new OperationalReport(
                org,
                from,
                to,
                ZONE.getId(),
                start,
                end,
                "operations-1",
                OffsetDateTime.now(ZoneOffset.UTC),
                municipality,
                route,
                vehicle,
                r.getLong("completed"),
                r.getLong("measured"),
                r.getLong("completed") - r.getLong("measured"),
                r.getBigDecimal("km"),
                r.getBigDecimal("seconds"),
                r.getLong("receipts"),
                r.getBigDecimal("kg"),
                r.getLong("unlinked")));
  }

  /** Rejects absent or foreign historical dimensions without exposing another organization. */
  private void require(String query, MapSqlParameterSource p) {
    if (jdbc.queryForObject(query, p, Long.class) == 0)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report filter not found");
  }
}
