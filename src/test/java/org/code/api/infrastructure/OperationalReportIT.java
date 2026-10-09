package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.ports.TokenPort;
import org.code.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Verifies approved live fact grains, Brazilian period boundaries and effective scope in
 * PostgreSQL.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@AutoConfigureMockMvc
class OperationalReportIT extends PostgresIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired TokenPort tokens;
  private static final AtomicInteger CODES = new AtomicInteger(70000);

  record Fixture(UUID actor, UUID org, UUID city, UUID route, UUID vehicle, UUID driver) {}

  /**
   * Local midnight includes the first instant, excludes the next day and excludes unfinished work.
   */
  @Test
  void usesBrazilianCompletionBoundariesAndNullCompleteness() throws Exception {
    var f = fixture();
    execution(f, "2026-10-08T03:00:00Z", "1.125");
    execution(f, "2026-10-09T02:59:59Z", null);
    execution(f, "2026-10-09T03:00:00Z", "100");
    var r = report(f, "", 200);
    assertThat(r.path("completedExecutions").asLong()).isEqualTo(2);
    assertThat(r.path("measuredExecutions").asLong()).isEqualTo(1);
    assertThat(r.path("missingDistanceExecutions").asLong()).isEqualTo(1);
    assertThat(r.path("reportedDistanceKm").asText()).isEqualTo("1.125");
    assertThat(new BigDecimal(r.path("durationSeconds").asText()))
        .isEqualByComparingTo("120.000000");
    assertThat(r.path("startInclusive").asText()).startsWith("2026-10-08T00:00:00-03:00");
  }

  /**
   * Receipt dates are independent of execution dates and live edits are reflected without fan-out.
   */
  @Test
  void sumsActiveReceiptsAndRecalculatesWithoutTeamFanOut() throws Exception {
    var f = fixture();
    UUID first = receipt(f, "2026-10-08T03:00:00Z", "1.2345");
    receipt(f, "2026-10-09T02:59:59Z", "2.3456");
    receipt(f, "2026-10-09T03:00:00Z", "999");
    UUID helper = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO team_member(id,organization_id,name,role,creator_id) VALUES (?,?,'Helper','HELPER',?)",
        helper,
        f.org(),
        f.actor());
    jdbc.update(
        "INSERT INTO collection_team(collection_id,team_member_id) VALUES (?,?),(?,?)",
        first,
        helper,
        first,
        f.driver());
    var r = report(f, "", 200);
    assertThat(r.path("collectionCount").asLong()).isEqualTo(2);
    assertThat(r.path("unlinkedCollections").asLong()).isEqualTo(2);
    assertThat(r.path("collectedWeightKg").asText()).isEqualTo("3.5801");
    assertThat(report(f, "&municipalityId=" + f.city(), 200).path("collectionCount").asLong())
        .isZero();
    jdbc.update("UPDATE collection SET total_weight_kg=5.0001 WHERE id=?", first);
    assertThat(report(f, "", 200).path("collectedWeightKg").asText()).isEqualTo("7.3457");
    jdbc.update("UPDATE collection SET is_active=false WHERE id=?", first);
    assertThat(report(f, "", 200).path("collectedWeightKg").asText()).isEqualTo("2.3456");
  }

  /**
   * Reported zero differs from an unknown distance; inactive dimensions still filter historical
   * facts.
   */
  @Test
  void retainsHistoricalFiltersAndDistinguishesZeroFromMissing() throws Exception {
    var f = fixture();
    assertThat(report(f, "", 200).path("reportedDistanceKm").isNull()).isTrue();
    execution(f, "2026-10-08T12:00:00Z", "0");
    jdbc.update("UPDATE route SET is_active=false WHERE id=?", f.route());
    jdbc.update("UPDATE vehicle SET is_active=false WHERE id=?", f.vehicle());
    var r = report(f, "&routeId=" + f.route() + "&vehicleId=" + f.vehicle(), 200);
    assertThat(r.path("completedExecutions").asLong()).isEqualTo(1);
    assertThat(new BigDecimal(r.path("reportedDistanceKm").asText())).isEqualByComparingTo("0");
    assertThat(r.path("missingDistanceExecutions").asLong()).isZero();
  }

  /**
   * Missing authentication/scope, foreign filters and invalid or excessive periods are rejected.
   */
  @Test
  void enforcesScopeAndBoundedInputs() throws Exception {
    var f = fixture();
    var other = fixture();
    execution(other, "2026-10-08T12:00:00Z", "99");
    assertThat(report(f, "", 200).path("completedExecutions").asLong()).isZero();
    report(f, "&routeId=" + other.route(), 404);
    report(f, "&vehicleId=" + other.vehicle(), 404);
    mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/reports/operations?from=2026-10-09&to=2026-10-08")
                .header("Authorization", token(f))
                .header("X-Organization-Id", f.org()))
        .andExpect(status().isBadRequest());
    mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/reports/operations?from=2025-01-01&to=2026-10-08")
                .header("Authorization", token(f))
                .header("X-Organization-Id", f.org()))
        .andExpect(status().isBadRequest());
    mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/reports/operations?from=2026-10-08&to=2026-10-08"))
        .andExpect(status().isUnauthorized());
    jdbc.update(
        "UPDATE organization_membership SET is_active=false WHERE organization_id=? AND user_id=?",
        f.org(),
        f.actor());
    report(f, "", 404);
  }

  /**
   * Linked receipts use snapshot geography even when execution completion is outside the period.
   */
  @Test
  void keepsLinkedReceiptAndExecutionGrainsIndependent() throws Exception {
    var f = fixture();
    UUID e = execution(f, "2026-10-07T12:00:00Z", "7");
    for (String kg : new String[] {"1.0001", "2.0002"}) {
      jdbc.update(
          "INSERT INTO collection(id,organization_id,realization_date,total_weight_kg,vehicle_id,driver_id,creator_id,route_execution_id,departure_at,arrival_at,distance_km) SELECT ?,organization_id,'2026-10-08T12:00:00Z'::timestamptz,?::numeric,vehicle_id,driver_id,?,id,started_at,finished_at,distance_km FROM route_execution WHERE id=?",
          UUID.randomUUID(),
          kg,
          f.actor(),
          e);
    }
    var r = report(f, "&routeId=" + f.route() + "&municipalityId=" + f.city(), 200);
    assertThat(r.path("completedExecutions").asLong()).isZero();
    assertThat(r.path("collectionCount").asLong()).isEqualTo(2);
    assertThat(r.path("unlinkedCollections").asLong()).isZero();
    assertThat(r.path("collectedWeightKg").asText()).isEqualTo("3.0003");
  }

  private JsonNode report(Fixture f, String filters, int expected) throws Exception {
    var r =
        mvc.perform(
                MockMvcRequestBuilders.get(
                        "/api/v1/reports/operations?from=2026-10-08&to=2026-10-08" + filters)
                    .header("Authorization", token(f))
                    .header("X-Organization-Id", f.org()))
            .andExpect(status().is(expected))
            .andReturn()
            .getResponse();
    return json.readTree(r.getContentAsString());
  }

  private String token(Fixture f) {
    return "Bearer "
        + tokens.createToken(
            Session.builder()
                .id(f.actor())
                .email(f.actor() + "@example.test")
                .userRole(UserRole.REPRESENTATIVE)
                .build());
  }

  private UUID execution(Fixture f, String finish, String distance) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO route_execution(id,organization_id,route_id,route_version,route_name,municipality_id,municipality_name,municipality_ibge_code,municipality_state_code,planned_start_at,vehicle_id,vehicle_license_plate,driver_id,driver_name,status,started_at,finished_at,distance_km,created_by,updated_by) SELECT ?,?,?,0,'Snapshot',?,'City',ibge_code,'PR',?::timestamptz,?,'ABC1D23',?,'Driver','COMPLETED',?::timestamptz-interval '60 seconds',?::timestamptz,?::numeric,?,? FROM municipality WHERE id=?",
        id,
        f.org(),
        f.route(),
        f.city(),
        finish,
        f.vehicle(),
        f.driver(),
        finish,
        finish,
        distance,
        f.actor(),
        f.actor(),
        f.city());
    return id;
  }

  private UUID receipt(Fixture f, String date, String kg) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO collection(id,organization_id,realization_date,total_weight_kg,vehicle_id,driver_id,creator_id) VALUES (?,?,?::timestamptz,?::numeric,?,?,?)",
        id,
        f.org(),
        date,
        kg,
        f.vehicle(),
        f.driver(),
        f.actor());
    return id;
  }

  private Fixture fixture() {
    UUID actor = UUID.randomUUID(),
        org = UUID.randomUUID(),
        city = UUID.randomUUID(),
        route = UUID.randomUUID(),
        vehicle = UUID.randomUUID(),
        driver = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users(id,email,password_hash,full_name,user_role) VALUES (?,?,'fixture','Reporter','REPRESENTATIVE')",
        actor,
        actor + "@example.test");
    jdbc.update(
        "INSERT INTO organization(id,name,organization_type,created_by) VALUES (?,'Report fixture','Association',?)",
        org,
        actor);
    jdbc.update(
        "INSERT INTO organization_membership(organization_id,user_id,role,granted_by) VALUES (?,?,'MEMBER',?)",
        org,
        actor,
        actor);
    jdbc.update(
        "INSERT INTO municipality(id,ibge_code,name,state_code,created_by) VALUES (?,?,'City','PR',?)",
        city,
        "41" + CODES.incrementAndGet(),
        actor);
    jdbc.update(
        "INSERT INTO route(id,organization_id,municipality_id,name,status,created_by,updated_by) VALUES (?,?,?,'Plan','ACTIVE',?,?)",
        route,
        org,
        city,
        actor,
        actor);
    jdbc.update(
        "INSERT INTO vehicle(id,organization_id,license_plate,creator_id) VALUES (?,?,'ABC1D23',?)",
        vehicle,
        org,
        actor);
    jdbc.update(
        "INSERT INTO team_member(id,organization_id,name,role,creator_id) VALUES (?,?,'Driver','DRIVER',?)",
        driver,
        org,
        actor);
    return new Fixture(actor, org, city, route, vehicle, driver);
  }
}
