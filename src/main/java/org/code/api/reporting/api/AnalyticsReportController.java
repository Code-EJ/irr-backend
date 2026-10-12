package org.code.api.reporting.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.reporting.application.AnalyticsReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/**
 * Official Swagger contract for owner-approved analytics-1.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
@Tag(name = "Native analytics")
public class AnalyticsReportController {
  private final AnalyticsReportService service;

  /** Reads weighted, scope-authorized metrics with explicit unavailable results. */
  @GetMapping("/analytics")
  @Operation(
      summary = "Read organization MVP analytics",
      description =
          "Live analytics-1; inclusive America/Sao_Paulo dates, at most 366 days. Completed execution cohorts, unique declared sections, weighted linked-receipt productivity and current POSTED sale-date revenue. Revenue unavailable under municipality/route/vehicle filters. Backend ratios rounded HALF_UP to six decimals.")
  public AnalyticsReport get(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) UUID municipalityId,
      @RequestParam(required = false) UUID routeId,
      @RequestParam(required = false) UUID vehicleId) {
    return service.get(from, to, municipalityId, routeId, vehicleId);
  }
}
