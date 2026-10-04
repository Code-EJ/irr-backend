package org.code.api.routes.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reusable route planning and immutable audit values, separate from dated execution assignments.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public final class RouteContract {
  private RouteContract() {}

  /** Draft plans cannot be scheduled; active plans remain subject to soft lifecycle. */
  public enum RouteStatus {
    DRAFT,
    ACTIVE
  }

  /**
   * Order is supplied by the typed array; a missing street declares neighborhood-level coverage.
   */
  @Schema(name = "RouteAreaRequest")
  public record AreaRequest(@NotNull UUID neighborhoodId, @Size(max = 255) String street) {}

  /** Replaces a complete plan; updates require the last returned version. */
  @Schema(name = "RouteRequest")
  public record Request(
      @NotBlank @Size(max = 150) String name,
      @NotNull UUID municipalityId,
      @NotNull RouteStatus status,
      @PositiveOrZero Long version,
      @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal plannedDistanceKm,
      @Positive Integer plannedDurationMinutes,
      @NotEmpty @Size(max = 200) List<@NotNull @Valid AreaRequest> areas) {}

  /** Preserves the planned area identity for later execution snapshots. */
  @Schema(name = "RouteAreaResponse")
  public record Area(
      UUID id, UUID neighborhoodId, String neighborhoodName, int sequence, String street) {}

  /** Includes explicit ownership, territory, version and audit metadata. */
  @Schema(name = "RouteResponse")
  public record Response(
      UUID id,
      UUID organizationId,
      UUID municipalityId,
      String municipalityName,
      String municipalityIbgeCode,
      String municipalityStateCode,
      String name,
      RouteStatus status,
      boolean isActive,
      long version,
      BigDecimal plannedDistanceKm,
      Integer plannedDurationMinutes,
      List<Area> areas,
      UUID createdBy,
      UUID updatedBy,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}

  /** Records a transactional actor/action/version fact without exposing credentials. */
  @Schema(name = "RouteEventResponse")
  public record Event(
      UUID id,
      UUID routeId,
      UUID executionId,
      UUID actorId,
      String action,
      long version,
      OffsetDateTime recordedAt) {}
}
