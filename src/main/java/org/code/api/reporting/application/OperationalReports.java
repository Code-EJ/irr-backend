package org.code.api.reporting.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.code.api.domain.ports.OrganizationScope;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Live operational summaries with explicit UTC business-date ranges and no stale result cache.
 * Current document status determines inclusion; reversed processing and sales are excluded.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
@lombok.RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class OperationalReports {
  private final JdbcTemplate jdbc;
  private final OrganizationScope scope;

  /** Period values are cumulative within inclusive UTC dates; stock is the current snapshot. */
  public record Summary(
      LocalDate from,
      LocalDate to,
      BigDecimal collectionWeightKg,
      BigDecimal donationWeightKg,
      BigDecimal sortedWeightKg,
      BigDecimal rejectedWeightKg,
      BigDecimal pressedWeightKg,
      BigDecimal soldWeightKg,
      BigDecimal revenueBrl,
      BigDecimal currentStockWeightKg,
      BigDecimal currentStockVolumeM3) {}

  /** Returns a repeatable live snapshot for at most 366 days. */
  public Summary summary(LocalDate from, LocalDate to) {
    LocalDate end = to == null ? LocalDate.now(ZoneOffset.UTC) : to,
        start = from == null ? end.withDayOfMonth(1) : from;
    if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > 365)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Report range must contain between 1 and 366 inclusive UTC days");
    UUID org = scope.organizationId();
    OffsetDateTime after = start.atStartOfDay().atOffset(ZoneOffset.UTC),
        before = end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
    BigDecimal collected =
        period(
            "SELECT COALESCE(SUM(total_weight_kg),0) FROM collection WHERE is_active AND"
                + " organization_id=? AND realization_date>=? AND realization_date<?",
            org,
            after,
            before);
    BigDecimal donated =
        period(
            "SELECT COALESCE(SUM(total_weight_kg),0) FROM donation WHERE is_active AND"
                + " organization_id=? AND donation_date>=? AND donation_date<?",
            org,
            after,
            before);
    BigDecimal sorted =
        period(
            "SELECT COALESCE(SUM(i.weight_kg-i.reject_weight_kg),0) FROM sorted_item i JOIN sorting"
                + " s ON s.id=i.sorting_id WHERE s.status='POSTED' AND s.organization_id=? AND"
                + " s.sorting_date>=? AND s.sorting_date<?",
            org,
            after,
            before);
    BigDecimal rejected =
        period(
            "SELECT COALESCE(SUM(i.reject_weight_kg),0) FROM sorted_item i JOIN sorting s ON"
                + " s.id=i.sorting_id WHERE s.status='POSTED' AND s.organization_id=? AND"
                + " s.sorting_date>=? AND s.sorting_date<?",
            org,
            after,
            before);
    BigDecimal pressed =
        period(
            "SELECT COALESCE(SUM(i.weight_kg),0) FROM pressed_bale i JOIN pressing p ON"
                + " p.id=i.pressing_id WHERE p.status='POSTED' AND p.organization_id=? AND"
                + " p.pressing_date>=? AND p.pressing_date<?",
            org,
            after,
            before);
    BigDecimal sold =
        period(
            "SELECT COALESCE(SUM(i.weight_kg),0) FROM sale_item i JOIN sale s ON s.id=i.sale_id"
                + " WHERE s.status='POSTED' AND i.is_active AND s.organization_id=? AND"
                + " s.sale_date>=? AND s.sale_date<?",
            org,
            after,
            before);
    BigDecimal revenue =
        period(
            "SELECT COALESCE(SUM(total_value),0) FROM sale WHERE status='POSTED' AND"
                + " organization_id=? AND sale_date>=? AND sale_date<?",
            org,
            after,
            before);
    var stock =
        jdbc.queryForMap(
            "SELECT COALESCE(SUM(current_weight_kg),0) w,COALESCE(SUM(current_volume_m3),0) v FROM"
                + " inventory_balance WHERE organization_id=?",
            org);
    return new Summary(
        start,
        end,
        collected,
        donated,
        sorted,
        rejected,
        pressed,
        sold,
        revenue,
        (BigDecimal) stock.get("w"),
        (BigDecimal) stock.get("v"));
  }

  private BigDecimal period(String sql, UUID org, OffsetDateTime after, OffsetDateTime before) {
    return jdbc.queryForObject(sql, BigDecimal.class, org, after, before);
  }
}
