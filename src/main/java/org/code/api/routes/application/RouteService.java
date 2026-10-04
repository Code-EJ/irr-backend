package org.code.api.routes.application;

import static org.code.api.routes.application.RouteAccess.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.routes.api.RouteContract.*;
import org.code.api.routes.infrastructure.RouteStore;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains reusable plans independently of dated execution and recorded collection history.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class RouteService {
  private final JdbcTemplate jdbc;
  private final RouteAccess access;
  private final RouteStore store;
  private final GeographyService geography;

  /** Lists scoped plans using stable name/identity ordering and explicit inactive inclusion. */
  @Transactional(readOnly = true)
  public Page<Response> list(boolean includeInactive, Pageable page) {
    UUID org = access.organization();
    String filter = " WHERE organization_id=?" + (includeInactive ? "" : " AND is_active");
    var ids =
        jdbc.query(
            "SELECT id FROM route" + filter + " ORDER BY name,id LIMIT ? OFFSET ?",
            (r, n) -> r.getObject(1, UUID.class),
            org,
            page.getPageSize(),
            page.getOffset());
    return new PageImpl<>(
        ids.stream().map(id -> store.route(org, id)).toList(),
        page,
        jdbc.queryForObject("SELECT count(*) FROM route" + filter, Long.class, org));
  }

  /** Reads one owned plan, retaining inactive records for historical navigation. */
  @Transactional(readOnly = true)
  public Response get(UUID id) {
    return store.route(access.organization(), id);
  }

  /** Creates a complete plan after validating all coverage against one explicit municipality. */
  @Transactional
  public Response create(Request request) {
    UUID org = access.manager();
    validate(request);
    UUID id = UUID.randomUUID();
    UUID actor = access.actor();
    try {
      jdbc.update(
          "INSERT INTO route(id,organization_id,municipality_id,name,status,planned_distance_km,planned_duration_minutes,created_by,updated_by) VALUES (?,?,?,?,?,?,?,?,?)",
          id,
          org,
          request.municipalityId(),
          request.name().strip(),
          request.status().name(),
          request.plannedDistanceKm(),
          request.plannedDurationMinutes(),
          actor,
          actor);
    } catch (DuplicateKeyException duplicate) {
      throw conflict("An active route with this name already exists in the municipality");
    }
    areas(org, id, request);
    store.event(org, id, null, actor, "CREATE", 0);
    return store.route(org, id);
  }

  /** Replaces current planning only; already scheduled execution snapshots remain unchanged. */
  @Transactional
  public Response update(UUID id, Request request) {
    UUID org = access.manager();
    Response old = store.route(org, id);
    version(request.version(), old.version());
    if (!old.isActive()) throw conflict("Inactive routes cannot be edited");
    if (!old.municipalityId().equals(request.municipalityId()))
      throw conflict(
          "Route municipality is immutable; create a distinct route for another territory");
    validate(request);
    try {
      jdbc.update(
          "UPDATE route SET name=?,status=?,planned_distance_km=?,planned_duration_minutes=?,version=version+1,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE organization_id=? AND id=?",
          request.name().strip(),
          request.status().name(),
          request.plannedDistanceKm(),
          request.plannedDurationMinutes(),
          access.actor(),
          org,
          id);
    } catch (DuplicateKeyException duplicate) {
      throw conflict("An active route with this name already exists in the municipality");
    }
    jdbc.update(
        "UPDATE route_area SET is_active=false WHERE organization_id=? AND route_id=? AND is_active",
        org,
        id);
    areas(org, id, request);
    store.event(org, id, null, access.actor(), "UPDATE", old.version() + 1);
    return store.route(org, id);
  }

  /** Soft-deactivates a plan only when no planned or in-progress execution still depends on it. */
  @Transactional
  public void deactivate(UUID id, Long expectedVersion) {
    UUID org = access.manager();
    Response old = store.route(org, id);
    version(expectedVersion, old.version());
    if (!old.isActive()) throw conflict("Route is already inactive");
    if (Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM route_execution WHERE organization_id=? AND route_id=? AND status IN ('PLANNED','IN_PROGRESS'))",
            Boolean.class,
            org,
            id)))
      throw conflict("Cancel or complete open executions before deactivating the route");
    jdbc.update(
        "UPDATE route SET is_active=false,version=version+1,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE organization_id=? AND id=?",
        access.actor(),
        org,
        id);
    store.event(org, id, null, access.actor(), "DEACTIVATE", old.version() + 1);
  }

  /** Returns bounded actor/action history for an owned plan, including execution events. */
  @Transactional(readOnly = true)
  public Page<Event> events(UUID id, Pageable page) {
    UUID org = access.organization();
    store.route(org, id);
    var rows =
        jdbc.query(
            "SELECT * FROM route_event WHERE organization_id=? AND route_id=? ORDER BY recorded_at,id LIMIT ? OFFSET ?",
            (r, n) ->
                new Event(
                    r.getObject("id", UUID.class),
                    id,
                    r.getObject("execution_id", UUID.class),
                    r.getObject("actor_id", UUID.class),
                    r.getString("action"),
                    r.getLong("version"),
                    r.getObject("recorded_at", OffsetDateTime.class)),
            org,
            id,
            page.getPageSize(),
            page.getOffset());
    return new PageImpl<>(
        rows,
        page,
        jdbc.queryForObject(
            "SELECT count(*) FROM route_event WHERE organization_id=? AND route_id=?",
            Long.class,
            org,
            id));
  }

  private void validate(Request request) {
    geography.requireMunicipality(request.municipalityId());
    var keys = new HashSet<String>();
    for (var area : request.areas()) {
      if (!Boolean.TRUE.equals(
          jdbc.queryForObject(
              "SELECT EXISTS(SELECT 1 FROM neighborhood WHERE id=? AND municipality_id=?)",
              Boolean.class,
              area.neighborhoodId(),
              request.municipalityId())))
        throw invalid("Every neighborhood must belong to the route municipality");
      String key =
          area.neighborhoodId()
              + ":"
              + (street(area.street()) == null
                  ? ""
                  : street(area.street()).toLowerCase(Locale.ROOT));
      if (!keys.add(key)) throw invalid("Duplicate neighborhood/street coverage is not allowed");
    }
  }

  private void areas(UUID org, UUID id, Request request) {
    int sequence = 0;
    for (var area : request.areas())
      jdbc.update(
          "INSERT INTO route_area(id,organization_id,route_id,municipality_id,neighborhood_id,sequence,street) VALUES (?,?,?,?,?,?,?)",
          UUID.randomUUID(),
          org,
          id,
          request.municipalityId(),
          area.neighborhoodId(),
          ++sequence,
          street(area.street()));
  }

  private String street(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
