package org.code.api.routes.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Dated execution commands and immutable plan/fleet/team context snapshots.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public final class RouteExecutionContract {
  private RouteExecutionContract() {}

  /** Terminal states retain history and cannot be reopened by current commands. */
  public enum ExecutionStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
  }

  /** Assigns existing fleet and staff to an active reusable plan. */
  @Schema(name = "RouteExecutionRequest")
  public record Request(
      @NotNull UUID routeId,
      @NotNull OffsetDateTime plannedStartAt,
      @NotNull UUID vehicleId,
      @NotNull UUID driverId,
      @Size(max = 100) Set<@NotNull UUID> teamMemberIds) {}

  /** Version precondition prevents stale lifecycle commands from overwriting current work. */
  @Schema(name = "RouteExecutionVersionCommand")
  public record VersionCommand(@NotNull @PositiveOrZero Long version) {}

  /** An omitted manual measurement remains unavailable rather than becoming zero. */
  @Schema(name = "RouteExecutionCompletionCommand")
  public record CompletionCommand(
      @NotNull @PositiveOrZero Long version,
      @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal distanceKm) {}

  /** Cancellation requires an explicit reason and retains all previously recorded context. */
  @Schema(name = "RouteExecutionCancellationCommand")
  public record CancellationCommand(
      @NotNull @PositiveOrZero Long version, @NotBlank @Size(max = 500) String reason) {}

  /** Records operator-declared coverage only during actual execution. */
  @Schema(name = "RouteExecutionCoverageCommand")
  public record CoverageCommand(@NotNull @PositiveOrZero Long version, @NotNull Boolean covered) {}

  /** Coverage labels are copied from the plan, not reread from mutable current geography. */
  @Schema(name = "RouteExecutionAreaResponse")
  public record Area(
      UUID id,
      UUID sourceAreaId,
      UUID neighborhoodId,
      String neighborhoodName,
      int sequence,
      String street,
      boolean covered) {}

  /** Existing staff identity and display/role are preserved at assignment time. */
  @Schema(name = "RouteExecutionTeamResponse")
  public record Team(UUID teamMemberId, String name, String role) {}

  /** Dated operational facts retain both planned and actual values and exact units. */
  @Schema(name = "RouteExecutionResponse")
  public record Response(
      UUID id,
      UUID organizationId,
      UUID routeId,
      long routeVersion,
      String routeName,
      UUID municipalityId,
      String municipalityName,
      String municipalityIbgeCode,
      String municipalityStateCode,
      BigDecimal plannedDistanceKm,
      Integer plannedDurationMinutes,
      OffsetDateTime plannedStartAt,
      ExecutionStatus status,
      long version,
      UUID vehicleId,
      String vehicleLicensePlate,
      UUID driverId,
      String driverName,
      OffsetDateTime startedAt,
      OffsetDateTime finishedAt,
      OffsetDateTime cancelledAt,
      String cancellationReason,
      BigDecimal distanceKm,
      List<Area> areas,
      List<Team> team,
      UUID createdBy,
      UUID updatedBy,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}
}
