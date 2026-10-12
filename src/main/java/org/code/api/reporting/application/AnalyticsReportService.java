package org.code.api.reporting.application;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.reporting.api.AnalyticsReport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads independent fact grains in one PostgreSQL snapshot and computes backend-only ratios.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AnalyticsReportService {
  private final ReportQueryScope scope;
  private final NamedParameterJdbcTemplate jdbc;
  private static final BigDecimal HUNDRED = new BigDecimal("100");

  /** Uses immutable completion cohorts and separately bounded posted-sale dates. */
  @Transactional(readOnly = true)
  public AnalyticsReport get(
      LocalDate from, LocalDate to, UUID municipality, UUID route, UUID vehicle) {
    var p = scope.parameters(from, to, municipality, route, vehicle);
    boolean salesAvailable = municipality == null && route == null && vehicle == null;
    p.addValue("salesAvailable", salesAvailable);
    return jdbc.queryForObject(
        SQL,
        p,
        (r, n) -> {
          long completed = r.getLong("completed");
          long declared = r.getLong("declared");
          long conflicts = r.getLong("conflicts");
          return new AnalyticsReport(
              (UUID) p.getValue("org"),
              from,
              to,
              ReportQueryScope.ZONE.getId(),
              (OffsetDateTime) p.getValue("start"),
              (OffsetDateTime) p.getValue("end"),
              "analytics-1",
              OffsetDateTime.now(ZoneOffset.UTC),
              municipality,
              route,
              vehicle,
              completed,
              r.getLong("unlinked"),
              comparison(r, "distance", "km", completed),
              comparison(r, "duration", "s", completed),
              new AnalyticsReport.Coverage(
                  r.getLong("coverage_executions"),
                  completed - r.getLong("coverage_executions"),
                  declared,
                  r.getLong("covered"),
                  r.getLong("duplicates"),
                  conflicts,
                  conflicts > 0
                      ? null
                      : AnalyticsMath.ratio(
                          BigDecimal.valueOf(r.getLong("covered")),
                          BigDecimal.valueOf(declared),
                          HUNDRED)),
              productivity(r, "hour", "kg/h", new BigDecimal("3600"), completed),
              productivity(r, "km", "kg/km", BigDecimal.ONE, completed),
              new AnalyticsReport.Revenue(
                  "BRL",
                  salesAvailable,
                  salesAvailable ? "AVAILABLE" : "NO_SALE_EXECUTION_PROVENANCE",
                  salesAvailable ? r.getLong("sales") : null,
                  salesAvailable ? r.getLong("drafts") : null,
                  salesAvailable ? r.getLong("reversed") : null,
                  salesAvailable ? r.getBigDecimal("revenue") : null));
        });
  }

  /** Compares matched populations and preserves an undefined percentage at a zero baseline. */
  private AnalyticsReport.Comparison comparison(ResultSet r, String key, String unit, long total)
      throws SQLException {
    long count = r.getLong(key + "_count");
    var planned = r.getBigDecimal(key + "_planned");
    var actual = r.getBigDecimal(key + "_actual");
    var delta = planned == null || actual == null ? null : actual.subtract(planned);
    return new AnalyticsReport.Comparison(
        unit,
        count,
        total - count,
        planned,
        actual,
        delta,
        delta == null ? null : delta.abs(),
        AnalyticsMath.ratio(delta, planned, HUNDRED));
  }

  /** Aggregates the numerator and denominator from precisely the same eligible executions. */
  private AnalyticsReport.Productivity productivity(
      ResultSet r, String key, String unit, BigDecimal multiplier, long total) throws SQLException {
    long count = r.getLong(key + "_count");
    var kg = r.getBigDecimal(key + "_kg");
    var denominator = r.getBigDecimal(key + "_denominator");
    return new AnalyticsReport.Productivity(
        unit,
        count,
        r.getLong(key + "_receipts"),
        total - count,
        r.getLong("without_receipts"),
        r.getLong(key + "_missing"),
        r.getLong(key + "_zero"),
        kg,
        denominator,
        AnalyticsMath.ratio(kg, denominator, multiplier));
  }

  private static final String SQL =
      """
      WITH executions AS (
        SELECT id,planned_distance_km,planned_duration_minutes*60::numeric planned_seconds,
               distance_km,extract(epoch FROM finished_at-started_at) seconds
        FROM route_execution WHERE organization_id=:org AND status='COMPLETED'
          AND finished_at>=:start AND finished_at<:end
          AND (CAST(:municipality AS uuid) IS NULL OR municipality_id=:municipality)
          AND (CAST(:route AS uuid) IS NULL OR route_id=:route)
          AND (CAST(:vehicle AS uuid) IS NULL OR vehicle_id=:vehicle)
      ), facts AS (
        SELECT e.*,count(c.id) receipts,sum(c.total_weight_kg) kg
        FROM executions e LEFT JOIN collection c
          ON c.organization_id=:org AND c.is_active AND c.route_execution_id=e.id
        GROUP BY e.id,e.planned_distance_km,e.planned_seconds,e.distance_km,e.seconds
      ), totals AS (
        SELECT count(*) completed,
          count(*) FILTER(WHERE planned_distance_km IS NOT NULL AND distance_km IS NOT NULL) distance_count,
          sum(planned_distance_km) FILTER(WHERE distance_km IS NOT NULL) distance_planned,
          sum(distance_km) FILTER(WHERE planned_distance_km IS NOT NULL) distance_actual,
          count(*) FILTER(WHERE planned_seconds IS NOT NULL AND seconds IS NOT NULL) duration_count,
          sum(planned_seconds) FILTER(WHERE seconds IS NOT NULL) duration_planned,
          sum(seconds) FILTER(WHERE planned_seconds IS NOT NULL) duration_actual,
          count(*) FILTER(WHERE receipts=0) without_receipts,
          count(*) FILTER(WHERE receipts>0 AND seconds>0) hour_count,
          coalesce(sum(receipts) FILTER(WHERE seconds>0),0) hour_receipts,
          sum(kg) FILTER(WHERE receipts>0 AND seconds>0) hour_kg,
          sum(seconds) FILTER(WHERE receipts>0 AND seconds>0) hour_denominator,
          count(*) FILTER(WHERE receipts>0 AND seconds IS NULL) hour_missing,
          count(*) FILTER(WHERE receipts>0 AND seconds=0) hour_zero,
          count(*) FILTER(WHERE receipts>0 AND distance_km>0) km_count,
          coalesce(sum(receipts) FILTER(WHERE distance_km>0),0) km_receipts,
          sum(kg) FILTER(WHERE receipts>0 AND distance_km>0) km_kg,
          sum(distance_km) FILTER(WHERE receipts>0 AND distance_km>0) km_denominator,
          count(*) FILTER(WHERE receipts>0 AND distance_km IS NULL) km_missing,
          count(*) FILTER(WHERE receipts>0 AND distance_km=0) km_zero FROM facts
      ), unique_sections AS (
        SELECT a.execution_id,a.source_area_id,count(*) copies,
               bool_and(a.covered) all_covered,bool_or(a.covered) any_covered
        FROM route_execution_area a JOIN executions e ON e.id=a.execution_id
        WHERE a.organization_id=:org GROUP BY a.execution_id,a.source_area_id
      ), coverage AS (
        SELECT count(*) declared,count(*) FILTER(WHERE all_covered) covered,
          count(DISTINCT execution_id) coverage_executions,
          coalesce(sum(copies-1),0) duplicates,
          count(*) FILTER(WHERE all_covered<>any_covered) conflicts FROM unique_sections
      ), revenue AS (
        SELECT count(*) FILTER(WHERE status='POSTED') sales,
          count(*) FILTER(WHERE status='DRAFT') drafts,
          count(*) FILTER(WHERE status='REVERSED') reversed,
          coalesce(sum(total_value) FILTER(WHERE status='POSTED'),0) revenue
        FROM sale WHERE organization_id=:org AND is_active AND :salesAvailable
          AND sale_date>=:start AND sale_date<:end
      ), quality AS (
        SELECT count(*) unlinked FROM collection WHERE organization_id=:org AND is_active
          AND route_execution_id IS NULL AND realization_date>=:start AND realization_date<:end
          AND (CAST(:vehicle AS uuid) IS NULL OR vehicle_id=:vehicle)
      ) SELECT * FROM totals CROSS JOIN coverage CROSS JOIN revenue CROSS JOIN quality
      """;
}
