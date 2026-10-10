package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises analytics fact grains, quality exclusions, Brazilian dates and live corrections.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@AutoConfigureMockMvc
class AnalyticsReportIT extends PostgresIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired TokenPort tokens;
  private static final AtomicInteger CODES = new AtomicInteger(80000);

  record Fixture(
      UUID actor, UUID org, UUID city, UUID route, UUID vehicle, UUID driver, UUID area) {}

  /**
   * Empty cohorts have unavailable ratios while an empty sale population has known zero revenue.
   */
  @Test
  void emptyAndMissingPopulationsRemainExplicit() throws Exception {
    var f = fixture();
    var r = report(f, "", 200);
    assertThat(r.path("distanceComparison").path("planned").isNull()).isTrue();
    assertThat(r.path("coverage").path("percentage").isNull()).isTrue();
    assertThat(r.path("hourlyProductivity").path("value").isNull()).isTrue();
    decimal(r.path("revenue"), "amount", "0");
    execution(f, null, null, null, 0, "2026-10-08T12:00:00Z", new boolean[0]);
    var missing = report(f, "", 200);
    assertThat(missing.path("distanceComparison").path("excludedExecutions").asInt()).isEqualTo(1);
    assertThat(missing.path("hourlyProductivity").path("withoutLinkedCollections").asInt())
        .isEqualTo(1);
  }

  /** Verifies weighted facts and live corrections on one connection under both prepared modes. */
  @ParameterizedTest
  @ValueSource(strings = {"force_custom_plan", "force_generic_plan"})
  @Transactional
  void usesWeightedMatchedCohortsAndLiveReceiptCorrections(String planMode) throws Exception {
    jdbc.queryForObject("SELECT set_config('plan_cache_mode', ?, true)", String.class, planMode);
    var f = fixture();
    var first = execution(f, "10", 60, "10", 3600, "2026-10-08T12:00:00Z", new boolean[] {true});
    var second = execution(f, "20", 60, "30", 7200, "2026-10-08T18:00:00Z", new boolean[] {false});
    var c = receipt(f, first, "10");
    receipt(f, first, "10");
    receipt(f, second, "100");
    JsonNode r = null;
    // Cross JDBC's prepare threshold on the transaction-bound connection before assertions.
    for (int i = 0; i < 7; i++) r = report(f, "", 200);
    decimal(r.path("distanceComparison"), "planned", "30");
    decimal(r.path("distanceComparison"), "actual", "40");
    decimal(r.path("distanceComparison"), "absoluteDeviation", "10");
    decimal(r.path("distanceComparison"), "percentageDeviation", "33.333333");
    decimal(r.path("durationComparison"), "percentageDeviation", "50");
    decimal(r.path("hourlyProductivity"), "value", "40");
    decimal(r.path("distanceProductivity"), "value", "3");
    assertThat(r.path("hourlyProductivity").path("collectionCount").asInt()).isEqualTo(3);
    decimal(r.path("coverage"), "percentage", "50");
    jdbc.update("UPDATE collection SET is_active=false WHERE id=?", c);
    decimal(report(f, "", 200).path("hourlyProductivity"), "value", "36.666667");
  }

  /** Each denominator has a separate missing/zero exclusion cohort and no average of ratios. */
  @Test
  void excludesMissingAndZeroDenominatorsWithoutLosingKnownZero() throws Exception {
    var f = fixture();
    var zero = execution(f, "0", 1, "0", 0, "2026-10-08T12:00:00Z", new boolean[0]);
    var missing = execution(f, "5", 1, null, 60, "2026-10-08T13:00:00Z", new boolean[0]);
    receipt(f, zero, "5");
    receipt(f, missing, "0");
    var r = report(f, "", 200);
    assertThat(r.path("distanceComparison").path("percentageDeviation").isNull()).isTrue();
    decimal(r.path("distanceComparison"), "actual", "0");
    assertThat(r.path("distanceProductivity").path("value").isNull()).isTrue();
    assertThat(r.path("distanceProductivity").path("missingDenominatorExecutions").asInt())
        .isEqualTo(1);
    assertThat(r.path("distanceProductivity").path("zeroDenominatorExecutions").asInt())
        .isEqualTo(1);
    decimal(r.path("hourlyProductivity"), "value", "0");
  }

  /** Coverage deduplicates source identity and refuses contradictory duplicate states. */
  @Test
  void deduplicatesSectionsAndDetectsConflictingStates() throws Exception {
    var f = fixture();
    execution(f, "1", 1, "1", 60, "2026-10-08T12:00:00Z", new boolean[] {true, true});
    var r = report(f, "", 200).path("coverage");
    assertThat(r.path("declaredSections").asInt()).isEqualTo(1);
    assertThat(r.path("duplicateRows").asInt()).isEqualTo(1);
    decimal(r, "percentage", "100");
    execution(f, "1", 1, "1", 60, "2026-10-08T13:00:00Z", new boolean[] {true, false});
    var conflict = report(f, "", 200).path("coverage");
    assertThat(conflict.path("declaredSections").asInt()).isEqualTo(2);
    assertThat(conflict.path("conflictingSections").asInt()).isEqualTo(1);
    assertThat(conflict.path("percentage").isNull()).isTrue();
  }

  /** Historical revenue recalculates after reversal; route attribution remains unavailable. */
  @Test
  void recalculatesRevenueAfterReversalAndRejectsGuessedGeography() throws Exception {
    var f = fixture();
    var sale = sale(f, "POSTED", "2026-10-08T03:00:00Z", "12.34");
    sale(f, "DRAFT", "2026-10-08T12:00:00Z", "999");
    sale(f, "REVERSED", "2026-10-08T12:00:00Z", "777");
    sale(f, "POSTED", "2026-10-09T03:00:00Z", "555");
    var r = report(f, "", 200).path("revenue");
    decimal(r, "amount", "12.34");
    assertThat(r.path("consideredSales").asInt()).isEqualTo(1);
    assertThat(r.path("excludedDraftSales").asInt()).isEqualTo(1);
    assertThat(r.path("excludedReversedSales").asInt()).isEqualTo(1);
    jdbc.update(
        "UPDATE sale SET status='REVERSED',updated_at='2026-10-10T12:00:00Z' WHERE id=?", sale);
    decimal(report(f, "", 200).path("revenue"), "amount", "0");
    var unavailable = report(f, "&routeId=" + f.route(), 200).path("revenue");
    assertThat(unavailable.path("available").asBoolean()).isFalse();
    assertThat(unavailable.path("amount").isNull()).isTrue();
  }

  /**
   * Completion boundaries, ownership and revocation remain enforced by shared report validation.
   */
  @Test
  void enforcesBrazilianDatesAndEffectiveScope() throws Exception {
    var f = fixture();
    var other = fixture();
    execution(f, "1", 1, "1", 60, "2026-10-08T03:00:00Z", new boolean[0]);
    execution(f, "1", 1, "1", 60, "2026-10-09T03:00:00Z", new boolean[0]);
    execution(other, "1", 1, "1", 60, "2026-10-08T12:00:00Z", new boolean[0]);
    assertThat(report(f, "", 200).path("completedExecutions").asInt()).isEqualTo(1);
    report(f, "&routeId=" + other.route(), 404);
    report(f, "&vehicleId=" + other.vehicle(), 404);
    mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/reports/analytics?from=2026-10-09&to=2026-10-08")
                .header("Authorization", token(f))
                .header("X-Organization-Id", f.org()))
        .andExpect(status().isBadRequest());
    mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/reports/analytics?from=2026-10-08&to=2026-10-08"))
        .andExpect(status().isUnauthorized());
    jdbc.update(
        "UPDATE organization_membership SET is_active=false WHERE organization_id=? AND user_id=?",
        f.org(),
        f.actor());
    report(f, "", 404);
  }

  private void decimal(JsonNode r, String name, String expected) {
    assertThat(r.path(name).isTextual()).isTrue();
    assertThat(new BigDecimal(r.path(name).asText())).isEqualByComparingTo(expected);
  }

  private JsonNode report(Fixture f, String filters, int expected) throws Exception {
    var result =
        mvc.perform(
                MockMvcRequestBuilders.get(
                        "/api/v1/reports/analytics?from=2026-10-08&to=2026-10-08" + filters)
                    .header("Authorization", token(f))
                    .header("X-Organization-Id", f.org()))
            .andExpect(status().is(expected))
            .andReturn()
            .getResponse();
    return json.readTree(result.getContentAsString());
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

  private UUID receipt(Fixture f, UUID e, String kg) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO collection(id,organization_id,realization_date,total_weight_kg,vehicle_id,driver_id,creator_id,route_execution_id,departure_at,arrival_at,distance_km) SELECT ?,organization_id,'2026-10-07T12:00:00Z'::timestamptz,?::numeric,vehicle_id,driver_id,?,id,started_at,finished_at,distance_km FROM route_execution WHERE id=?",
        id,
        kg,
        f.actor(),
        e);
    return id;
  }

  private UUID execution(
      Fixture f,
      String planned,
      Integer minutes,
      String distance,
      int seconds,
      String finish,
      boolean[] coverage) {
    UUID e = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO route_execution(id,organization_id,route_id,route_version,route_name,municipality_id,municipality_name,municipality_ibge_code,municipality_state_code,planned_distance_km,planned_duration_minutes,planned_start_at,vehicle_id,vehicle_license_plate,driver_id,driver_name,created_by,updated_by) SELECT ?,?,?,0,'Snapshot',?,'City',ibge_code,'PR',?::numeric,?,?::timestamptz,?,'ABC1D23',?,'Driver',?,? FROM municipality WHERE id=?",
        e,
        f.org(),
        f.route(),
        f.city(),
        planned,
        minutes,
        finish,
        f.vehicle(),
        f.driver(),
        f.actor(),
        f.actor(),
        f.city());
    UUID[] ids = new UUID[coverage.length];
    for (int i = 0; i < coverage.length; i++) {
      ids[i] = UUID.randomUUID();
      jdbc.update(
          "INSERT INTO route_execution_area(id,organization_id,execution_id,route_id,source_area_id,neighborhood_id,neighborhood_name,sequence) SELECT ?,?,?,?,id,neighborhood_id,'Area',? FROM route_area WHERE id=?",
          ids[i],
          f.org(),
          e,
          f.route(),
          i + 1,
          f.area());
    }
    jdbc.update(
        "UPDATE route_execution SET status='IN_PROGRESS',started_at=?::timestamptz-(? * interval '1 second') WHERE id=?",
        finish,
        seconds,
        e);
    for (int i = 0; i < coverage.length; i++)
      jdbc.update("UPDATE route_execution_area SET covered=? WHERE id=?", coverage[i], ids[i]);
    jdbc.update(
        "UPDATE route_execution SET status='COMPLETED',finished_at=?::timestamptz,distance_km=?::numeric WHERE id=?",
        finish,
        distance,
        e);
    return e;
  }

  private UUID sale(Fixture f, String state, String date, String amount) {
    UUID buyer = UUID.randomUUID(), doc = UUID.randomUUID(), id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO buyer(id,organization_id,name,creator_id) VALUES (?,?,'Buyer',?)",
        buyer,
        f.org(),
        f.actor());
    jdbc.update(
        "INSERT INTO attachment(id,organization_id,file_name,file_type,storage_url,creator_id) VALUES (?,?,'fixture.pdf','application/pdf',?,?)",
        doc,
        f.org(),
        doc.toString(),
        f.actor());
    jdbc.update(
        "INSERT INTO sale(id,organization_id,buyer_id,nfe_attachment_id,sale_date,total_value,status,creator_id) VALUES (?,?,?,?,?::timestamptz,?::numeric,?,?)",
        id,
        f.org(),
        buyer,
        doc,
        date,
        amount,
        state,
        f.actor());
    return id;
  }

  private Fixture fixture() {
    UUID actor = UUID.randomUUID(),
        org = UUID.randomUUID(),
        city = UUID.randomUUID(),
        route = UUID.randomUUID(),
        vehicle = UUID.randomUUID(),
        driver = UUID.randomUUID(),
        area = UUID.randomUUID(),
        neighborhood = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users(id,email,password_hash,full_name,user_role) VALUES (?,?,'fixture','Reporter','REPRESENTATIVE')",
        actor,
        actor + "@example.test");
    jdbc.update(
        "INSERT INTO organization(id,name,organization_type,created_by) VALUES (?,'Analytics fixture','Association',?)",
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
        "INSERT INTO neighborhood(id,municipality_id,name,created_by) VALUES (?,?,'Area',?)",
        neighborhood,
        city,
        actor);
    jdbc.update(
        "INSERT INTO route(id,organization_id,municipality_id,name,status,created_by,updated_by) VALUES (?,?,?,'Plan','ACTIVE',?,?)",
        route,
        org,
        city,
        actor,
        actor);
    jdbc.update(
        "INSERT INTO route_area(id,organization_id,route_id,municipality_id,neighborhood_id,sequence) VALUES (?,?,?,?,?,1)",
        area,
        org,
        route,
        city,
        neighborhood);
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
    return new Fixture(actor, org, city, route, vehicle, driver, area);
  }
}
