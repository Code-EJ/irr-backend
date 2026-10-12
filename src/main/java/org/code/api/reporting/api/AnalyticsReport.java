package org.code.api.reporting.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Auditable live analytics-1 cohorts; exact decimals serialize as strings.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record AnalyticsReport(
    UUID organizationId,
    LocalDate from,
    LocalDate to,
    String timezone,
    OffsetDateTime startInclusive,
    OffsetDateTime endExclusive,
    String definitionVersion,
    OffsetDateTime generatedAt,
    @Schema(nullable = true) UUID municipalityId,
    @Schema(nullable = true) UUID routeId,
    @Schema(nullable = true) UUID vehicleId,
    long completedExecutions,
    long unlinkedCollectionsInPeriod,
    Comparison distanceComparison,
    Comparison durationComparison,
    Coverage coverage,
    Productivity hourlyProductivity,
    Productivity distanceProductivity,
    Revenue revenue) {

  /** Matched sums and deviations all use the same population. */
  @Schema(name = "AnalyticsComparison")
  public record Comparison(
      String unit,
      long consideredExecutions,
      long excludedExecutions,
      @Schema(nullable = true) BigDecimal planned,
      @Schema(nullable = true) BigDecimal actual,
      @Schema(nullable = true) BigDecimal signedDeviation,
      @Schema(nullable = true) BigDecimal absoluteDeviation,
      @Schema(nullable = true) BigDecimal percentageDeviation) {}

  /** Unique execution/section pairs; conflicting duplicates invalidate the percentage. */
  @Schema(name = "AnalyticsCoverage")
  public record Coverage(
      long consideredExecutions,
      long excludedExecutions,
      long declaredSections,
      long coveredSections,
      long duplicateRows,
      long conflictingSections,
      @Schema(nullable = true) BigDecimal percentage) {}

  /** Weighted ratio with exact numerator, denominator and complete exclusion partition. */
  @Schema(name = "AnalyticsProductivity")
  public record Productivity(
      String unit,
      long consideredExecutions,
      long collectionCount,
      long excludedExecutions,
      long withoutLinkedCollections,
      long missingDenominatorExecutions,
      long zeroDenominatorExecutions,
      @Schema(nullable = true) BigDecimal collectedWeightKg,
      @Schema(nullable = true) BigDecimal denominator,
      @Schema(nullable = true) BigDecimal value) {}

  /** Sale-date totals cannot be attributed to geographic dimensions without provenance. */
  @Schema(name = "AnalyticsRevenue")
  public record Revenue(
      String currency,
      boolean available,
      String availabilityReason,
      @Schema(nullable = true) Long consideredSales,
      @Schema(nullable = true) Long excludedDraftSales,
      @Schema(nullable = true) Long excludedReversedSales,
      @Schema(nullable = true) BigDecimal amount) {}
}
