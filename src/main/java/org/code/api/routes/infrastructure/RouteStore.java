package org.code.api.routes.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.routes.api.RouteContract;
import org.code.api.routes.api.RouteExecutionContract;
import org.code.api.routes.application.RouteAccess;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reads scoped operational records and their bounded immutable context without new domain layers.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
@RequiredArgsConstructor
public class RouteStore {
  private final JdbcTemplate jdbc;

  /** Reads a plan, including inactive plans retained for execution history. */
  public RouteContract.Response route(UUID organization, UUID id) {
    var records =
        jdbc.query(
            "SELECT r.*,m.name municipality_name,m.ibge_code,m.state_code FROM route r JOIN municipality m ON m.id=r.municipality_id WHERE r.organization_id=? AND r.id=?",
            (r, n) -> routeRow(r),
            organization,
            id);
    if (records.isEmpty()) throw RouteAccess.missing("Route");
    return records.getFirst();
  }

  private RouteContract.Response routeRow(ResultSet r) throws SQLException {
    UUID id = r.getObject("id", UUID.class);
    var areas =
        jdbc.query(
            "SELECT a.*,n.name neighborhood_name FROM route_area a JOIN neighborhood n ON n.id=a.neighborhood_id WHERE a.route_id=? AND a.is_active ORDER BY a.sequence",
            (a, n) ->
                new RouteContract.Area(
                    a.getObject("id", UUID.class),
                    a.getObject("neighborhood_id", UUID.class),
                    a.getString("neighborhood_name"),
                    a.getInt("sequence"),
                    a.getString("street")),
            id);
    return new RouteContract.Response(
        id,
        r.getObject("organization_id", UUID.class),
        r.getObject("municipality_id", UUID.class),
        r.getString("municipality_name"),
        r.getString("ibge_code"),
        r.getString("state_code"),
        r.getString("name"),
        RouteContract.RouteStatus.valueOf(r.getString("status")),
        r.getBoolean("is_active"),
        r.getLong("version"),
        r.getBigDecimal("planned_distance_km"),
        r.getObject("planned_duration_minutes", Integer.class),
        areas,
        r.getObject("created_by", UUID.class),
        r.getObject("updated_by", UUID.class),
        r.getObject("created_at", OffsetDateTime.class),
        r.getObject("updated_at", OffsetDateTime.class));
  }

  /** Reads preserved execution facts without replacing snapshots with current labels. */
  public RouteExecutionContract.Response execution(UUID organization, UUID id) {
    var records =
        jdbc.query(
            "SELECT * FROM route_execution WHERE organization_id=? AND id=?",
            (r, n) -> executionRow(r),
            organization,
            id);
    if (records.isEmpty()) throw RouteAccess.missing("Route execution");
    return records.getFirst();
  }

  private RouteExecutionContract.Response executionRow(ResultSet r) throws SQLException {
    UUID id = r.getObject("id", UUID.class);
    var areas =
        jdbc.query(
            "SELECT * FROM route_execution_area WHERE execution_id=? ORDER BY sequence",
            (a, n) ->
                new RouteExecutionContract.Area(
                    a.getObject("id", UUID.class),
                    a.getObject("source_area_id", UUID.class),
                    a.getObject("neighborhood_id", UUID.class),
                    a.getString("neighborhood_name"),
                    a.getInt("sequence"),
                    a.getString("street"),
                    a.getBoolean("covered")),
            id);
    var team =
        jdbc.query(
            "SELECT * FROM route_execution_team WHERE execution_id=? ORDER BY member_name,team_member_id",
            (a, n) ->
                new RouteExecutionContract.Team(
                    a.getObject("team_member_id", UUID.class),
                    a.getString("member_name"),
                    a.getString("member_role")),
            id);
    return new RouteExecutionContract.Response(
        id,
        r.getObject("organization_id", UUID.class),
        r.getObject("route_id", UUID.class),
        r.getLong("route_version"),
        r.getString("route_name"),
        r.getObject("municipality_id", UUID.class),
        r.getString("municipality_name"),
        r.getString("municipality_ibge_code"),
        r.getString("municipality_state_code"),
        r.getBigDecimal("planned_distance_km"),
        r.getObject("planned_duration_minutes", Integer.class),
        r.getObject("planned_start_at", OffsetDateTime.class),
        RouteExecutionContract.ExecutionStatus.valueOf(r.getString("status")),
        r.getLong("version"),
        r.getObject("vehicle_id", UUID.class),
        r.getString("vehicle_license_plate"),
        r.getObject("driver_id", UUID.class),
        r.getString("driver_name"),
        r.getObject("started_at", OffsetDateTime.class),
        r.getObject("finished_at", OffsetDateTime.class),
        r.getObject("cancelled_at", OffsetDateTime.class),
        r.getString("cancellation_reason"),
        r.getBigDecimal("distance_km"),
        areas,
        team,
        r.getObject("created_by", UUID.class),
        r.getObject("updated_by", UUID.class),
        r.getObject("created_at", OffsetDateTime.class),
        r.getObject("updated_at", OffsetDateTime.class));
  }

  /** Appends actor/action/version history in the enclosing command transaction. */
  public void event(
      UUID organization, UUID route, UUID execution, UUID actor, String action, long version) {
    jdbc.update(
        "INSERT INTO route_event(id,organization_id,route_id,execution_id,actor_id,action,version,recorded_at) VALUES (?,?,?,?,?,?,?,clock_timestamp())",
        UUID.randomUUID(),
        organization,
        route,
        execution,
        actor,
        action,
        version);
  }
}
