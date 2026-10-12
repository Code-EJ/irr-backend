package org.code.api.reporting.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Exact live totals at execution and receipt grain.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Schema(name = "OperationalReport")
public record OperationalReport(
    UUID organizationId,
    LocalDate from,
    LocalDate to,
    String timezone,
    OffsetDateTime startInclusive,
    OffsetDateTime endExclusive,
    String definitionVersion,
    OffsetDateTime generatedAt,
    @Schema(nullable = true, description = "Applied municipality filter; null when unrestricted")
        UUID municipalityId,
    @Schema(nullable = true, description = "Applied route filter; null when unrestricted")
        UUID routeId,
    @Schema(nullable = true, description = "Applied vehicle filter; null when unrestricted")
        UUID vehicleId,
    long completedExecutions,
    long measuredExecutions,
    long missingDistanceExecutions,
    @Schema(
            nullable = true,
            description = "Sum of reported km; null when no execution has a measurement")
        BigDecimal reportedDistanceKm,
    @Schema(description = "Exact sum of finished_at minus started_at, in seconds")
        BigDecimal durationSeconds,
    long collectionCount,
    BigDecimal collectedWeightKg,
    long unlinkedCollections) {}
