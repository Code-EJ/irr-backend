package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.ports.TokenPort;
import org.code.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies real PostgreSQL scope, version serialization, geographic constraints and history.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@AutoConfigureMockMvc
class RoutesIT extends PostgresIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired TokenPort tokens;
  private static final AtomicInteger CODES = new AtomicInteger(1000);

  record Fixture(
      UUID actor,
      UUID org,
      UUID municipality,
      UUID neighborhood,
      UUID vehicle,
      UUID driver,
      UUID helper,
      UserRole role) {}

  /** CRUD retains inactive plans, validates complete coverage and bounds pagination. */
  @Test
  void managesPlansWithVersionedLifecycle() throws Exception {
    var f = fixture(UserRole.REPRESENTATIVE, "MANAGER");
    var route = call(f, "POST", "/routes", plan(f, "Morning", null), 201);
    String path = "/routes/" + route.path("id").asText();
    assertThat(call(f, "GET", "/routes?page=0&size=1", null, 200).path("content")).hasSize(1);
    assertThat(call(f, "GET", path, null, 200).path("municipalityId").asText())
        .isEqualTo(f.municipality().toString());
    call(f, "PUT", path, plan(f, "Updated", 0L), 200);
    call(f, "PUT", path, plan(f, "Stale", 0L), 409);
    call(f, "DELETE", path + "?version=1", null, 204);
    assertThat(call(f, "GET", path, null, 200).path("isActive").asBoolean()).isFalse();
    assertThat(call(f, "GET", "/routes", null, 200).path("content")).isEmpty();
    assertThat(call(f, "GET", "/routes?includeInactive=true", null, 200).path("content"))
        .hasSize(1);
    assertThat(call(f, "GET", path + "/events", null, 200).path("content")).hasSize(3);
  }

  /**
   * Explicit municipality ownership and normalized coverage/name constraints reject invalid plans.
   */
  @Test
  void rejectsInvalidTerritoryAndDuplicatePlans() throws Exception {
    Fixture f = fixture(UserRole.REPRESENTATIVE, "MANAGER"),
        other = fixture(UserRole.REPRESENTATIVE, "MANAGER");
    var payload = plan(f, "Plan", null);
    payload.put("areas", List.of(Map.of("neighborhoodId", other.neighborhood())));
    call(f, "POST", "/routes", payload, 400);
    payload.put(
        "areas",
        List.of(
            Map.of("neighborhoodId", f.neighborhood()),
            Map.of("neighborhoodId", f.neighborhood())));
    call(f, "POST", "/routes", payload, 400);
    payload.put("areas", List.of());
    call(f, "POST", "/routes", payload, 400);
    call(f, "POST", "/routes", plan(f, "Plan", null), 201);
    call(f, "POST", "/routes", plan(f, " plan ", null), 409);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO neighborhood(id,municipality_id,name,created_by) VALUES (?,?,?,?)",
                    UUID.randomUUID(),
                    f.municipality(),
                    " area ",
                    f.actor()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  /**
   * Effective global/local authority remains unchanged and foreign identifiers are not disclosed.
   */
  @Test
  void enforcesOrganizationAndTerritoryPermissions() throws Exception {
    Fixture manager = fixture(UserRole.REPRESENTATIVE, "MANAGER"),
        member = fixture(UserRole.REPRESENTATIVE, "MEMBER"),
        global = fixture(UserRole.ADMINISTRATOR, null);
    var route = call(manager, "POST", "/routes", plan(manager, "Scoped", null), 201);
    call(member, "GET", "/routes/" + route.path("id").asText(), null, 404);
    call(member, "POST", "/routes", plan(member, "Forbidden", null), 403);
    call(
        manager,
        "POST",
        "/municipalities",
        Map.of("name", "Wrong prefix", "ibgeCode", "4100000", "stateCode", "MG"),
        403);
    call(
        global,
        "POST",
        "/municipalities",
        Map.of("name", "Wrong prefix", "ibgeCode", "4100000", "stateCode", "MG"),
        400);
    String code = "31" + String.format("%05d", CODES.incrementAndGet());
    var municipality =
        call(
            global,
            "POST",
            "/municipalities",
            Map.of("name", "Curated fixture", "ibgeCode", code, "stateCode", "MG"),
            201);
    call(
        global,
        "POST",
        "/neighborhoods",
        Map.of("name", "District", "municipalityId", municipality.path("id").asText()),
        201);
    call(global, "POST", "/routes", plan(global, "Global plan", null), 201);
    jdbc.update("UPDATE organization SET is_active=false WHERE id=?", manager.org());
    call(manager, "GET", "/routes", null, 404);
  }

  /** Snapshots survive plan changes; terminal lifecycle and declared coverage are immutable. */
  @Test
  void preservesSnapshotAndActualLifecycle() throws Exception {
    var f = fixture(UserRole.REPRESENTATIVE, "MANAGER");
    var route = call(f, "POST", "/routes", plan(f, "Original", null), 201);
    var execution = call(f, "POST", "/route-executions", assignment(f, route), 201);
    String path = "/route-executions/" + execution.path("id").asText();
    call(f, "DELETE", "/routes/" + route.path("id").asText() + "?version=0", null, 409);
    call(f, "PUT", "/routes/" + route.path("id").asText(), plan(f, "Changed", 0L), 200);
    jdbc.update("UPDATE team_member SET name='Changed driver' WHERE id=?", f.driver());
    var started = call(f, "POST", path + "/start", Map.of("version", 0), 200);
    assertThat(started.path("routeName").asText()).isEqualTo("Original");
    assertThat(started.path("driverName").asText()).isEqualTo("Driver");
    call(f, "POST", path + "/start", Map.of("version", 0), 409);
    call(
        f,
        "PUT",
        path + "/areas/" + started.path("areas").get(0).path("id").asText(),
        Map.of("version", 1, "covered", true),
        200);
    var completed =
        call(f, "POST", path + "/complete", Map.of("version", 2, "distanceKm", "12.345"), 200);
    assertThat(completed.path("distanceKm").asText()).isEqualTo("12.345");
    assertThat(completed.path("areas").get(0).path("covered").asBoolean()).isTrue();
    call(f, "POST", path + "/cancel", Map.of("version", 3, "reason", "Too late"), 409);
    call(f, "POST", path + "/complete", Map.of("version", 3), 409);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE route_execution SET route_name='Rewritten' WHERE id=?",
                    UUID.fromString(execution.path("id").asText())))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "DELETE FROM route_event WHERE execution_id=?",
                    UUID.fromString(execution.path("id").asText())))
        .isInstanceOf(DataIntegrityViolationException.class);
    call(f, "DELETE", "/routes/" + route.path("id").asText() + "?version=1", null, 204);
  }

  /**
   * Members can record operations but cannot schedule/cancel plans or mutate reference geography.
   */
  @Test
  void allowsMemberOperationsWithoutManagementPowers() throws Exception {
    var f = fixture(UserRole.REPRESENTATIVE, "MANAGER");
    var route = call(f, "POST", "/routes", plan(f, "Work", null), 201);
    var execution = call(f, "POST", "/route-executions", assignment(f, route), 201);
    String path = "/route-executions/" + execution.path("id").asText();
    jdbc.update(
        "UPDATE organization_membership SET role='MEMBER' WHERE user_id=? AND organization_id=?",
        f.actor(),
        f.org());
    call(f, "POST", "/route-executions", assignment(f, route), 403);
    call(f, "POST", path + "/cancel", Map.of("version", 0, "reason", "Forbidden"), 403);
    call(f, "POST", path + "/start", Map.of("version", 0), 200);
    assertThat(
            call(f, "POST", path + "/complete", Map.of("version", 1), 200)
                .path("distanceKm")
                .isNull())
        .isTrue();
    call(f, "GET", "/route-executions?status=COMPLETED", null, 200);
  }

  /** Foreign fleet/staff, a non-driver and draft plans cannot be scheduled. */
  @Test
  void validatesExecutionAssignmentsAndCancellation() throws Exception {
    Fixture f = fixture(UserRole.REPRESENTATIVE, "MANAGER"),
        other = fixture(UserRole.REPRESENTATIVE, "MANAGER");
    var route = call(f, "POST", "/routes", plan(f, "Assignment", null), 201);
    var assignment = assignment(f, route);
    assignment.put("vehicleId", other.vehicle());
    call(f, "POST", "/route-executions", assignment, 404);
    assignment.put("vehicleId", f.vehicle());
    assignment.put("driverId", f.helper());
    call(f, "POST", "/route-executions", assignment, 400);
    assignment.put("driverId", f.driver());
    assignment.put("teamMemberIds", List.of(f.driver()));
    call(f, "POST", "/route-executions", assignment, 400);
    var execution = call(f, "POST", "/route-executions", assignment(f, route), 201);
    String path = "/route-executions/" + execution.path("id").asText();
    call(f, "POST", path + "/complete", Map.of("version", 0), 409);
    call(f, "POST", path + "/cancel", Map.of("version", 0, "reason", " "), 400);
    call(f, "POST", path + "/cancel", Map.of("version", 0, "reason", "Weather"), 200);
    call(f, "POST", path + "/start", Map.of("version", 1), 409);
    var draft = plan(f, "Draft", null);
    draft.put("status", "DRAFT");
    var draftRoute = call(f, "POST", "/routes", draft, 201);
    call(f, "POST", "/route-executions", assignment(f, draftRoute), 409);
    call(f, "DELETE", "/vehicles/" + f.vehicle(), null, 409);
    call(f, "DELETE", "/team-members/" + f.driver(), null, 409);
    call(f, "DELETE", "/team-members/" + f.helper(), null, 409);
  }

  /** Concurrent stale plan replacements have exactly one winner and one version conflict. */
  @Test
  void serializesConcurrentVersions() throws Exception {
    var f = fixture(UserRole.REPRESENTATIVE, "MANAGER");
    var route = call(f, "POST", "/routes", plan(f, "Concurrent", null), 201);
    var barrier = new CyclicBarrier(2);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var attempts =
          List.of("One", "Two").stream()
              .map(
                  name ->
                      pool.submit(
                          () -> {
                            barrier.await(10, TimeUnit.SECONDS);
                            return mvc.perform(
                                    MockMvcRequestBuilders.put(
                                            "/api/v1/routes/" + route.path("id").asText())
                                        .header("Authorization", token(f))
                                        .header("X-Organization-Id", f.org())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json.writeValueAsString(plan(f, name, 0L))))
                                .andReturn()
                                .getResponse()
                                .getStatus();
                          }))
              .toList();
      assertThat(
              List.of(
                  attempts.get(0).get(20, TimeUnit.SECONDS),
                  attempts.get(1).get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 409);
    }
  }

  /** Collection references derive actual facts and reject mismatched or reassigned history. */
  @Test
  void linksCollectionsToCompletedExecution() throws Exception {
    var f = fixture(UserRole.REPRESENTATIVE, "MANAGER");
    var route = call(f, "POST", "/routes", plan(f, "Intake", null), 201);
    var execution = call(f, "POST", "/route-executions", assignment(f, route), 201);
    String path = "/route-executions/" + execution.path("id").asText();
    UUID category = UUID.randomUUID(), type = UUID.randomUUID(), material = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO material_category(id,organization_id,name,creator_id) VALUES (?,?,'Material',?)",
        category,
        f.org(),
        f.actor());
    jdbc.update(
        "INSERT INTO material_type(id,organization_id,category_id,name,creator_id) VALUES (?,?,?,'Type',?)",
        type,
        f.org(),
        category,
        f.actor());
    jdbc.update(
        "INSERT INTO material_subtype(id,organization_id,type_id,name,creator_id) VALUES (?,?,?,'Subtype',?)",
        material,
        f.org(),
        type,
        f.actor());
    var collection = new HashMap<String, Object>();
    collection.put("realizationDate", "2026-10-04T12:00:00Z");
    collection.put("totalWeightKg", "10");
    collection.put("vehicleId", f.vehicle());
    collection.put("driverId", f.driver());
    collection.put("teamMemberIds", List.of(f.helper()));
    collection.put("routeExecutionId", execution.path("id").asText());
    collection.put(
        "inputItems",
        List.of(Map.of("materialSubtypeId", material, "weightKg", "10", "volumeM3", "1")));
    call(f, "POST", "/collections", collection, 409);
    call(f, "POST", path + "/start", Map.of("version", 0), 200);
    var completed =
        call(f, "POST", path + "/complete", Map.of("version", 1, "distanceKm", "7.5"), 200);
    collection.put("teamMemberIds", List.of());
    call(f, "POST", "/collections", collection, 400);
    collection.put("teamMemberIds", List.of(f.helper()));
    var created = call(f, "POST", "/collections", collection, 201);
    assertThat(created.path("departureAt").asText())
        .isEqualTo(completed.path("startedAt").asText());
    assertThat(created.path("distanceKm").asText()).isEqualTo("7.500");
    String collectionPath = "/collections/" + created.path("id").asText();
    call(f, "PUT", collectionPath, collection, 200);
    collection.remove("routeExecutionId");
    call(f, "PUT", collectionPath, collection, 409);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE collection SET route_execution_id=NULL WHERE id=?",
                    UUID.fromString(created.path("id").asText())))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "DELETE FROM collection_team WHERE collection_id=?",
                    UUID.fromString(created.path("id").asText())))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private JsonNode call(Fixture f, String method, String path, Object body, int expected)
      throws Exception {
    var request =
        MockMvcRequestBuilders.request(HttpMethod.valueOf(method), "/api/v1" + path)
            .header("Authorization", token(f))
            .header("X-Organization-Id", f.org());
    if (body != null)
      request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    var response = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse();
    String result = response.getContentAsString();
    JsonNode payload = result.isBlank() ? json.createObjectNode() : json.readTree(result);
    if (expected == 201 && List.of("/municipalities", "/neighborhoods").contains(path)) {
      String location = "/api/v1" + path + "/" + payload.path("id").asText();
      assertThat(response.getHeader("Location")).isEqualTo(location);
      assertThat(call(f, "GET", path + "/" + payload.path("id").asText(), null, 200))
          .isEqualTo(payload);
    }
    return payload;
  }

  private String token(Fixture f) {
    return "Bearer "
        + tokens.createToken(
            Session.builder()
                .id(f.actor())
                .email(f.actor() + "@example.test")
                .userRole(f.role())
                .build());
  }

  private HashMap<String, Object> plan(Fixture f, String name, Long version) {
    var body = new HashMap<String, Object>();
    body.put("name", name);
    body.put("municipalityId", f.municipality());
    body.put("status", "ACTIVE");
    body.put("plannedDistanceKm", "10");
    body.put("plannedDurationMinutes", 90);
    body.put("areas", List.of(Map.of("neighborhoodId", f.neighborhood(), "street", "Street")));
    if (version != null) body.put("version", version);
    return body;
  }

  private HashMap<String, Object> assignment(Fixture f, JsonNode route) {
    return new HashMap<>(
        Map.of(
            "routeId",
            route.path("id").asText(),
            "plannedStartAt",
            "2026-10-05T09:00:00Z",
            "vehicleId",
            f.vehicle(),
            "driverId",
            f.driver(),
            "teamMemberIds",
            List.of(f.helper())));
  }

  private Fixture fixture(UserRole role, String localRole) {
    UUID actor = UUID.randomUUID(),
        org = UUID.randomUUID(),
        city = UUID.randomUUID(),
        area = UUID.randomUUID(),
        vehicle = UUID.randomUUID(),
        driver = UUID.randomUUID(),
        helper = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users(id,email,password_hash,full_name,user_role) VALUES (?,?,'fixture','Fixture',?)",
        actor,
        actor + "@example.test",
        role.name());
    jdbc.update(
        "INSERT INTO organization(id,name,organization_type,created_by) VALUES (?,'Routes fixture','Association',?)",
        org,
        actor);
    if (localRole != null)
      jdbc.update(
          "INSERT INTO organization_membership(organization_id,user_id,role,granted_by) VALUES (?,?,?,?)",
          org,
          actor,
          localRole,
          actor);
    String code = "41" + String.format("%05d", CODES.incrementAndGet());
    jdbc.update(
        "INSERT INTO municipality(id,ibge_code,name,state_code,created_by) VALUES (?,?,'Municipality','PR',?)",
        city,
        code,
        actor);
    jdbc.update(
        "INSERT INTO neighborhood(id,municipality_id,name,created_by) VALUES (?,?,'Area',?)",
        area,
        city,
        actor);
    jdbc.update(
        "INSERT INTO vehicle(id,organization_id,license_plate,creator_id) VALUES (?,?,'ABC1D23',?)",
        vehicle,
        org,
        actor);
    jdbc.update(
        "INSERT INTO team_member(id,organization_id,name,role,creator_id) VALUES (?,?,'Driver','DRIVER',?),(?,?,'Helper','HELPER',?)",
        driver,
        org,
        actor,
        helper,
        org,
        actor);
    return new Fixture(actor, org, city, area, vehicle, driver, helper, role);
  }
}
