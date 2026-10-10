package org.code.api.reporting.application;

import java.time.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.reporting.api.OperationalReport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final ReportQueryScope queryScope;
  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Validates effective scope, historical filters and dates before reading a consistent live
   * snapshot.
   */
  @Transactional(readOnly = true)
  public OperationalReport get(
      LocalDate from, LocalDate to, UUID municipality, UUID route, UUID vehicle) {
    var p = queryScope.parameters(from, to, municipality, route, vehicle);
    UUID org = (UUID) p.getValue("org");
    var start = (OffsetDateTime) p.getValue("start");
    var end = (OffsetDateTime) p.getValue("end");
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
}
