package org.code.api.reporting.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.reporting.application.OperationalReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/**
 * Serves the approved operational definition through the versioned contract.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
@Tag(name = "Operational reports")
public class OperationalReportController {
  private final OperationalReportService service;

  /** Reads a live report for inclusive Brazilian calendar dates and optional owned dimensions. */
  @GetMapping("/operations")
  @Operation(
      summary = "Read organization operational totals",
      description =
          "Live definition operations-1; America/Sao_Paulo dates inclusive, at most 366 days. Completed executions use completion time; active collections use realization time. Requires selected effective organization scope.")
  public OperationalReport operations(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) UUID municipalityId,
      @RequestParam(required = false) UUID routeId,
      @RequestParam(required = false) UUID vehicleId) {
    return service.get(from, to, municipalityId, routeId, vehicleId);
  }
}
