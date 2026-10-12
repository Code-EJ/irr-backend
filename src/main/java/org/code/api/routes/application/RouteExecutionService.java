package org.code.api.routes.application;

import static org.code.api.routes.application.RouteAccess.*;

import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.routes.api.RouteContract.RouteStatus;
import org.code.api.routes.api.RouteExecutionContract.*;
import org.code.api.routes.infrastructure.RouteStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records actual lifecycle and operator-declared coverage against immutable planning snapshots.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class RouteExecutionService {
  private final JdbcTemplate jdbc;
  private final RouteAccess access;
  private final RouteStore store;

  /** Lists bounded executions with optional plan and lifecycle filters. */
  @Transactional(readOnly = true)
  public Page<Response> list(UUID routeId, ExecutionStatus status, Pageable page) {
    UUID org = access.organization();
    String filter =
        " WHERE organization_id=? AND (?::uuid IS NULL OR route_id=?::uuid) AND (?::varchar IS NULL OR status=?::varchar)";
    String state = status == null ? null : status.name();
    var ids =
        jdbc.query(
            "SELECT id FROM route_execution"
                + filter
                + " ORDER BY planned_start_at DESC,id LIMIT ? OFFSET ?",
            (r, n) -> r.getObject(1, UUID.class),
            org,
            routeId,
            routeId,
            state,
            state,
            page.getPageSize(),
            page.getOffset());
    return new PageImpl<>(
        ids.stream().map(id -> store.execution(org, id)).toList(),
        page,
        jdbc.queryForObject(
            "SELECT count(*) FROM route_execution" + filter,
            Long.class,
            org,
            routeId,
            routeId,
            state,
            state));
  }

  /** Reads preserved planning, fleet, staff, coverage and actual facts for an owned execution. */
  @Transactional(readOnly = true)
  public Response get(UUID id) {
    return store.execution(access.organization(), id);
  }

  /** Schedules existing active fleet/staff and copies the complete current active plan. */
  @Transactional
  public Response create(Request request) {
    UUID org = access.manager();
    var route = store.route(org, request.routeId());
    if (!route.isActive() || route.status() != RouteStatus.ACTIVE)
      throw conflict("Only an active route can be scheduled");
    var vehicles =
        jdbc.query(
            "SELECT license_plate FROM vehicle WHERE organization_id=? AND id=? AND is_active",
            (r, n) -> r.getString(1),
            org,
            request.vehicleId());
    if (vehicles.isEmpty()) throw missing("Vehicle");
    Team driver = member(org, request.driverId());
    if (!"DRIVER".equals(driver.role()))
      throw invalid("Selected team member must have the DRIVER role");
    Set<UUID> team = request.teamMemberIds() == null ? Set.of() : request.teamMemberIds();
    if (team.contains(request.driverId()))
      throw invalid("The driver is assigned separately from the execution team");
    var members = team.stream().map(id -> member(org, id)).toList();
    UUID id = UUID.randomUUID();
    UUID actor = access.actor();
    jdbc.update(
        "INSERT INTO route_execution(id,organization_id,route_id,route_version,route_name,municipality_id,municipality_name,municipality_ibge_code,municipality_state_code,planned_distance_km,planned_duration_minutes,planned_start_at,vehicle_id,vehicle_license_plate,driver_id,driver_name,created_by,updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        id,
        org,
        route.id(),
        route.version(),
        route.name(),
        route.municipalityId(),
        route.municipalityName(),
        route.municipalityIbgeCode(),
        route.municipalityStateCode(),
        route.plannedDistanceKm(),
        route.plannedDurationMinutes(),
        request.plannedStartAt(),
        request.vehicleId(),
        vehicles.getFirst(),
        request.driverId(),
        driver.name(),
        actor,
        actor);
    for (var area : route.areas())
      jdbc.update(
          "INSERT INTO route_execution_area(id,organization_id,execution_id,route_id,source_area_id,neighborhood_id,neighborhood_name,sequence,street) VALUES (?,?,?,?,?,?,?,?,?)",
          UUID.randomUUID(),
          org,
          id,
          route.id(),
          area.id(),
          area.neighborhoodId(),
          area.neighborhoodName(),
          area.sequence(),
          area.street());
    for (var crew : members)
      jdbc.update(
          "INSERT INTO route_execution_team(organization_id,execution_id,team_member_id,member_name,member_role) VALUES (?,?,?,?,?)",
          org,
          id,
          crew.teamMemberId(),
          crew.name(),
          crew.role());
    store.event(org, route.id(), id, actor, "PLAN", 0);
    return store.execution(org, id);
  }

  /** Starts a planned execution using the server clock and current version. */
  @Transactional
  public Response start(UUID id, VersionCommand command) {
    UUID org = access.organization();
    var old = current(org, id, command.version(), ExecutionStatus.PLANNED);
    jdbc.update(
        "UPDATE route_execution SET status='IN_PROGRESS',started_at=clock_timestamp(),version=version+1,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE organization_id=? AND id=?",
        access.actor(),
        org,
        id);
    return recorded(org, old, "START");
  }

  /** Completes an in-progress execution without inventing unreported distance or coverage. */
  @Transactional
  public Response complete(UUID id, CompletionCommand command) {
    UUID org = access.organization();
    var old = current(org, id, command.version(), ExecutionStatus.IN_PROGRESS);
    jdbc.update(
        "UPDATE route_execution SET status='COMPLETED',finished_at=clock_timestamp(),distance_km=?,version=version+1,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE organization_id=? AND id=?",
        command.distanceKm(),
        access.actor(),
        org,
        id);
    return recorded(org, old, "COMPLETE");
  }

  /** Cancels only an open execution, requiring manager authority and an explicit reason. */
  @Transactional
  public Response cancel(UUID id, CancellationCommand command) {
    UUID org = access.manager();
    var old = store.execution(org, id);
    version(command.version(), old.version());
    if (old.status() != ExecutionStatus.PLANNED && old.status() != ExecutionStatus.IN_PROGRESS)
      throw conflict("Terminal executions cannot be cancelled");
    jdbc.update(
        "UPDATE route_execution SET status='CANCELLED',cancelled_at=clock_timestamp(),cancellation_reason=?,version=version+1,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE organization_id=? AND id=?",
        command.reason().strip(),
        access.actor(),
        org,
        id);
    return recorded(org, old, "CANCEL");
  }

  /** Changes one declared coverage flag only while the dated execution is in progress. */
  @Transactional
  public Response coverage(UUID id, UUID areaId, CoverageCommand command) {
    UUID org = access.organization();
    var old = current(org, id, command.version(), ExecutionStatus.IN_PROGRESS);
    int changed =
        jdbc.update(
            "UPDATE route_execution_area SET covered=? WHERE organization_id=? AND execution_id=? AND id=?",
            command.covered(),
            org,
            id,
            areaId);
    if (changed != 1) throw missing("Execution area");
    jdbc.update(
        "UPDATE route_execution SET version=version+1,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE organization_id=? AND id=?",
        access.actor(),
        org,
        id);
    return recorded(org, old, "COVERAGE");
  }

  private Response current(UUID org, UUID id, Long expected, ExecutionStatus required) {
    var old = store.execution(org, id);
    version(expected, old.version());
    if (old.status() != required)
      throw conflict("Execution must be " + required + " for this command");
    return old;
  }

  private Response recorded(UUID org, Response old, String action) {
    store.event(org, old.routeId(), old.id(), access.actor(), action, old.version() + 1);
    return store.execution(org, old.id());
  }

  private Team member(UUID org, UUID id) {
    var found =
        jdbc.query(
            "SELECT name,role FROM team_member WHERE organization_id=? AND id=? AND is_active",
            (r, n) -> new Team(id, r.getString("name"), r.getString("role")),
            org,
            id);
    if (found.isEmpty()) throw missing("Team member");
    return found.getFirst();
  }
}
