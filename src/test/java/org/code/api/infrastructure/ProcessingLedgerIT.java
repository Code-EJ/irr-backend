package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.ports.TokenPort;
import org.code.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises conserved quantities, atomic rollback, scope isolation and command replay in
 * PostgreSQL.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@AutoConfigureMockMvc
class ProcessingLedgerIT extends PostgresIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired TokenPort tokens;

  record Fixture(UUID actor, UUID org, UUID material, UUID input) {}

  /** Net sorting output is credited once; pressing changes volume without inventing mass. */
  @Test
  void conservesQuantitiesAndReplaysCommittedCommands() throws Exception {
    Fixture f = fixture();
    String key = UUID.randomUUID().toString();
    String body = sorting(f, "100", "10", "20", "2");
    String response = postCommand(f, "sorting", key, body, 201);
    assertThat(json.readTree(postCommand(f, "sorting", key, body, 201)))
        .isEqualTo(json.readTree(response));
    balance(f, "80", "8");
    assertThat(count(f, "stock_movement")).isEqualTo(1);
    UUID sorted =
        UUID.fromString(json.readTree(response).get("sortedItems").get(0).get("id").asText());
    String pressing = pressing(f, sorted, "50", "5", "1");
    postCommand(f, "pressing", UUID.randomUUID().toString(), pressing, 201);
    balance(f, "80", "4");
    assertThat(
            jdbc.queryForObject(
                "SELECT SUM(available_weight_kg) FROM stock_lot WHERE organization_id=?",
                BigDecimal.class,
                f.org()))
        .isEqualByComparingTo("80");
    postCommand(f, "pressing", UUID.randomUUID().toString(), pressing, 409);
    balance(f, "80", "4");
    assertThat(count(f, "stock_operation")).isEqualTo(2);
    postCommand(f, "sorting", key, sorting(f, "99", "10", "20", "2"), 409);
    assertThat(count(f, "command_receipt")).isEqualTo(2);
  }

  /** Exceeding raw intake and invalid compaction roll back all header and ledger effects. */
  @Test
  void failedCommandsLeaveNoPartialEffects() throws Exception {
    Fixture f = fixture();
    String key = UUID.randomUUID().toString();
    postCommand(f, "sorting", key, sorting(f, "101", "10", "0", "0"), 409);
    assertThat(count(f, "sorting")).isZero();
    assertThat(count(f, "stock_operation")).isZero();
    assertThat(count(f, "command_receipt")).isZero();
    String result = postCommand(f, "sorting", key, sorting(f, "100", "10", "0", "0"), 201);
    UUID sorted =
        UUID.fromString(json.readTree(result).get("sortedItems").get(0).get("id").asText());
    postCommand(
        f, "pressing", UUID.randomUUID().toString(), pressing(f, sorted, "50", "5", "6"), 400);
    assertThat(count(f, "pressing")).isZero();
    balance(f, "100", "10");
    postCommand(f, "sorting", UUID.randomUUID().toString(), sorting(f, "1", "1", "0", "0"), 409);
    balance(f, "100", "10");
  }

  /**
   * A membership is mandatory, and foreign source identifiers never resolve inside another
   * organization.
   */
  @Test
  void rejectsForeignSourcesAndMissingScope() throws Exception {
    Fixture f = fixture(), other = fixture();
    Fixture forged = new Fixture(f.actor(), f.org(), f.material(), other.input());
    postCommand(
        f, "sorting", UUID.randomUUID().toString(), sorting(forged, "10", "1", "0", "0"), 404);
    mvc.perform(get("/api/v1/sortings").header("Authorization", token(f)))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/sortings")
                .header("Authorization", token(f))
                .header("X-Organization-Id", other.org()))
        .andExpect(status().isNotFound());
    assertThat(count(f, "sorting")).isZero();
  }

  /** PostgreSQL refuses edits to the authoritative stock history. */
  @Test
  void committedHistoryIsImmutable() throws Exception {
    Fixture f = fixture();
    postCommand(f, "sorting", UUID.randomUUID().toString(), sorting(f, "100", "10", "0", "0"), 201);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE stock_movement SET weight_delta_kg=0 WHERE organization_id=?", f.org()))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> jdbc.update("DELETE FROM stock_operation WHERE organization_id=?", f.org()))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    balance(f, "100", "10");
  }

  /**
   * Competing sales cannot oversell a lot; reversal restores quantities and remains replay-safe.
   */
  @Test
  void concurrentSalesHaveOneWinnerAndAnAuditedReversal() throws Exception {
    Fixture f = fixture();
    postCommand(f, "sorting", UUID.randomUUID().toString(), sorting(f, "100", "10", "0", "0"), 201);
    UUID lot =
        jdbc.queryForObject(
            "SELECT id FROM stock_lot WHERE organization_id=?", UUID.class, f.org());
    UUID buyer = UUID.randomUUID(), invoice = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO buyer(id,organization_id,name,creator_id) VALUES (?,?,'Buyer',?)",
        buyer,
        f.org(),
        f.actor());
    jdbc.update(
        "INSERT INTO attachment(id,organization_id,file_name,file_type,storage_url,creator_id)"
            + " VALUES (?,?,'Invoice.pdf','application/pdf','fixture-only',?)",
        invoice,
        f.org(),
        f.actor());
    String draft =
        json.writeValueAsString(
            Map.of(
                "saleDate",
                "2026-09-27T10:00:00Z",
                "buyerId",
                buyer,
                "nfeAttachmentId",
                invoice,
                "items",
                List.of(
                    Map.of(
                        "stockLotId",
                        lot,
                        "materialSubtypeId",
                        f.material(),
                        "weightKg",
                        "60",
                        "volumeM3",
                        "6",
                        "unitPrice",
                        "2.125"))));
    UUID first =
        UUID.fromString(
            json.readTree(request(f, "POST", "/api/v1/sales", draft, 201)).get("id").asText());
    UUID second =
        UUID.fromString(
            json.readTree(request(f, "POST", "/api/v1/sales", draft, 201)).get("id").asText());
    balance(f, "100", "10");
    String transition = json.writeValueAsString(Map.of("version", 0));
    var gate = new java.util.concurrent.CyclicBarrier(2);
    java.util.List<org.springframework.test.web.servlet.MvcResult> results =
        new java.util.ArrayList<>();
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var futures =
          new java.util.ArrayList<
              java.util.concurrent.Future<org.springframework.test.web.servlet.MvcResult>>();
      for (UUID id : List.of(first, second))
        futures.add(
            pool.submit(
                () -> {
                  gate.await(10, java.util.concurrent.TimeUnit.SECONDS);
                  return mvc.perform(
                          post("/api/v1/sales/{id}/post", id)
                              .header("Authorization", token(f))
                              .header("X-Organization-Id", f.org())
                              .header("Idempotency-Key", UUID.randomUUID().toString())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(transition))
                      .andReturn();
                }));
      for (var future : futures) results.add(future.get(20, java.util.concurrent.TimeUnit.SECONDS));
    }
    assertThat(results.stream().map(r -> r.getResponse().getStatus()).toList())
        .containsExactlyInAnyOrder(200, 409);
    var posted =
        json.readTree(
            results.stream()
                .filter(r -> r.getResponse().getStatus() == 200)
                .findFirst()
                .orElseThrow()
                .getResponse()
                .getContentAsString());
    assertThat(new BigDecimal(posted.get("totalValue").asText())).isEqualByComparingTo("127.50");
    balance(f, "40", "4");
    UUID winner = UUID.fromString(posted.get("id").asText());
    request(f, "PUT", "/api/v1/sales/" + winner, draft, 409);
    String reversal = json.writeValueAsString(Map.of("version", posted.get("version").asLong()));
    String key = UUID.randomUUID().toString();
    for (int i = 0; i < 2; i++)
      mvc.perform(
              post("/api/v1/sales/{id}/reverse", winner)
                  .header("Authorization", token(f))
                  .header("X-Organization-Id", f.org())
                  .header("Idempotency-Key", key)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(reversal))
          .andExpect(status().isOk())
          .andExpect(jsonPath("status").value("REVERSED"));
    balance(f, "100", "10");
    assertThat(count(f, "stock_operation")).isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "SELECT SUM(weight_delta_kg) FROM stock_movement WHERE organization_id=?",
                BigDecimal.class,
                f.org()))
        .isEqualByComparingTo("100");
  }

  /** Draft replacement needs the current version and posting needs scoped fiscal evidence. */
  @Test
  void saleDraftLifecycleRequiresVersionAndInvoice() throws Exception {
    Fixture f = fixture();
    postCommand(f, "sorting", UUID.randomUUID().toString(), sorting(f, "100", "10", "0", "0"), 201);
    UUID
        lot =
            jdbc.queryForObject(
                "SELECT id FROM stock_lot WHERE organization_id=?", UUID.class, f.org()),
        buyer = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO buyer(id,organization_id,name,creator_id) VALUES (?,?,'Buyer',?)",
        buyer,
        f.org(),
        f.actor());
    var fields =
        new java.util.HashMap<String, Object>(
            Map.of(
                "saleDate",
                "2026-09-27T10:00:00Z",
                "buyerId",
                buyer,
                "items",
                List.of(
                    Map.of(
                        "stockLotId",
                        lot,
                        "materialSubtypeId",
                        f.material(),
                        "weightKg",
                        "20",
                        "volumeM3",
                        "2",
                        "unitPrice",
                        "1"))));
    var sale =
        json.readTree(request(f, "POST", "/api/v1/sales", json.writeValueAsString(fields), 201));
    String url = "/api/v1/sales/" + sale.get("id").asText();
    mvc.perform(
            post(url + "/post")
                .header("Authorization", token(f))
                .header("X-Organization-Id", f.org())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("version", 0))))
        .andExpect(status().isBadRequest());
    fields.put("version", 0);
    var updated = json.readTree(request(f, "PUT", url, json.writeValueAsString(fields), 200));
    assertThat(updated.get("version").asLong()).isEqualTo(1);
    request(f, "PUT", url, json.writeValueAsString(fields), 409);
    request(f, "DELETE", url + "?version=1", null, 204);
    request(f, "GET", url, null, 404);
    balance(f, "100", "10");
  }

  /** Managers maintain buyers and staff; ordinary members cannot mutate reference data. */
  @Test
  void partyCrudIsScopedAndManagerOnly() throws Exception {
    Fixture f = fixture();
    String buyer = json.writeValueAsString(Map.of("name", "Buyer", "document", "12345678901"));
    request(f, "POST", "/api/v1/buyers", buyer, 403);
    jdbc.update(
        "UPDATE organization_membership SET role='MANAGER' WHERE organization_id=?", f.org());
    for (String resource : List.of("buyers", "team-members")) {
      String body =
          resource.equals("buyers")
              ? buyer
              : json.writeValueAsString(Map.of("name", "Driver", "role", "DRIVER"));
      var created = json.readTree(request(f, "POST", "/api/v1/" + resource, body, 201));
      String url = "/api/v1/" + resource + "/" + created.get("id").asText();
      request(f, "GET", url, null, 200);
      request(f, "PUT", url, body, 200);
      request(fixture(), "GET", url, null, 404);
      request(f, "DELETE", url, null, 204);
      request(f, "GET", url, null, 404);
    }
  }

  /** Collection CRUD preserves processed source history and rejects foreign reference data. */
  @Test
  void collectionLifecyclePreservesProcessedInputs() throws Exception {
    Fixture f = fixture();
    jdbc.update(
        "UPDATE organization_membership SET role='MANAGER' WHERE organization_id=?", f.org());
    UUID vehicle = UUID.randomUUID(), driver = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO vehicle(id,organization_id,license_plate,creator_id) VALUES (?,?,'ABC1234',?)",
        vehicle,
        f.org(),
        f.actor());
    jdbc.update(
        "INSERT INTO team_member(id,organization_id,name,role,creator_id) VALUES"
            + " (?,?,'Driver','DRIVER',?)",
        driver,
        f.org(),
        f.actor());
    var fields =
        new java.util.HashMap<String, Object>(
            Map.of(
                "realizationDate",
                "2026-09-27T10:00:00Z",
                "totalWeightKg",
                "100",
                "vehicleId",
                vehicle,
                "driverId",
                driver,
                "inputItems",
                List.of(
                    Map.of(
                        "materialSubtypeId", f.material(), "weightKg", "100", "volumeM3", "10"))));
    fields.put("routeDescription", "North district route");
    fields.put("departureAt", "2026-09-27T08:00:00Z");
    fields.put("arrivalAt", "2026-09-27T09:00:00Z");
    fields.put("distanceKm", "12.345");
    String body = json.writeValueAsString(fields);
    var created = json.readTree(request(f, "POST", "/api/v1/collections", body, 201));
    assertThat(created.get("routeDescription").asText()).isEqualTo("North district route");
    assertThat(created.get("distanceKm").asText()).isEqualTo("12.345");
    fields.put("arrivalAt", "2026-09-27T07:00:00Z");
    request(f, "POST", "/api/v1/collections", json.writeValueAsString(fields), 400);
    fields.put("arrivalAt", "2026-09-27T09:00:00Z");
    String url = "/api/v1/collections/" + created.get("id").asText();
    var updated = json.readTree(request(f, "PUT", url, body, 200));
    UUID input = UUID.fromString(updated.get("inputItems").get(0).get("id").asText());
    postCommand(
        f,
        "sorting",
        UUID.randomUUID().toString(),
        sorting(new Fixture(f.actor(), f.org(), f.material(), input), "100", "10", "0", "0"),
        201);
    request(f, "PUT", url, body, 409);
    request(f, "DELETE", url, null, 409);
    request(f, "DELETE", "/api/v1/team-members/" + driver, null, 409);
    request(f, "DELETE", "/api/v1/vehicles/" + vehicle, null, 409);
    fields.put("driverId", UUID.randomUUID());
    request(f, "POST", "/api/v1/collections", json.writeValueAsString(fields), 404);
    fields.put("driverId", driver);
    fields.put("totalWeightKg", "99");
    request(f, "POST", "/api/v1/collections", json.writeValueAsString(fields), 400);
    var disposable = json.readTree(request(f, "POST", "/api/v1/collections", body, 201));
    request(f, "DELETE", "/api/v1/collections/" + disposable.get("id").asText(), null, 204);
  }

  /** Processing corrections must follow dependency order and preserve readable history. */
  @Test
  void reversesProcessingInDependencyOrderAndReconcilesStock() throws Exception {
    Fixture f = fixture();
    var sorted =
        json.readTree(
            postCommand(
                f,
                "sorting",
                UUID.randomUUID().toString(),
                sorting(f, "100", "10", "20", "2"),
                201));
    UUID item = UUID.fromString(sorted.get("sortedItems").get(0).get("id").asText());
    var pressed =
        json.readTree(
            postCommand(
                f,
                "pressing",
                UUID.randomUUID().toString(),
                pressing(f, item, "50", "5", "1"),
                201));
    String sortingUrl = "/api/v1/sortings/" + sorted.get("id").asText();
    String pressingUrl = "/api/v1/pressings/" + pressed.get("id").asText();
    reverse(f, sortingUrl, UUID.randomUUID().toString(), 409);
    reverse(f, pressingUrl, UUID.randomUUID().toString(), 200);
    balance(f, "80", "8");
    String key = UUID.randomUUID().toString();
    reverse(f, sortingUrl, key, 200);
    reverse(f, sortingUrl, key, 200);
    balance(f, "0", "0");
    assertThat(json.readTree(request(f, "GET", sortingUrl, null, 200)).get("status").asText())
        .isEqualTo("REVERSED");
    var reconciliation =
        json.readTree(request(f, "GET", "/api/v1/inventory/reconciliation", null, 200));
    assertThat(reconciliation.get(0).get("consistent").asBoolean()).isTrue();
    assertThat(
            json.readTree(request(f, "GET", "/api/v1/inventory/lots", null, 200))
                .get("totalElements")
                .asInt())
        .isZero();
    postCommand(f, "sorting", UUID.randomUUID().toString(), sorting(f, "100", "10", "0", "0"), 201);
    balance(f, "100", "10");
    jdbc.update(
        "UPDATE inventory_balance SET current_weight_kg=99 WHERE organization_id=?", f.org());
    assertThat(
            json.readTree(request(f, "GET", "/api/v1/inventory/reconciliation", null, 200))
                .get(0)
                .get("consistent")
                .asBoolean())
        .isFalse();
  }

  private void reverse(Fixture f, String path, String key, int expected) throws Exception {
    mvc.perform(
            post(path + "/reverse")
                .header("Authorization", token(f))
                .header("X-Organization-Id", f.org())
                .header("Idempotency-Key", key))
        .andExpect(status().is(expected));
  }

  /**
   * Backdated intake appears immediately in live reports, and donor addresses remain structured.
   */
  @Test
  void donorAddressesAndBackdatedReportsUseTheCurrentDatabase() throws Exception {
    Fixture f = fixture();
    jdbc.update(
        "UPDATE organization_membership SET role='MANAGER' WHERE organization_id=?", f.org());
    var address =
        Map.of(
            "line1",
            "10 Example Street",
            "city",
            "Example City",
            "region",
            "PR",
            "postalCode",
            "80000000",
            "countryCode",
            "BR");
    var donor =
        json.readTree(
            request(
                f,
                "POST",
                "/api/v1/donors",
                json.writeValueAsString(
                    Map.of(
                        "name",
                        "Address fixture",
                        "document",
                        "12345678902",
                        "donorType",
                        "PF",
                        "address",
                        address)),
                201));
    assertThat(donor.get("address").get("city").asText()).isEqualTo("Example City");
    java.time.LocalDate date = java.time.LocalDate.now(java.time.ZoneOffset.UTC).minusDays(10);
    String query = "?from=" + date + "&to=" + date;
    var before = json.readTree(request(f, "GET", "/api/v1/reports/summary" + query, null, 200));
    assertThat(new BigDecimal(before.get("donationWeightKg").asText())).isZero();
    String body =
        json.writeValueAsString(
            Map.of(
                "donationDate",
                date + "T12:00:00Z",
                "donorId",
                donor.get("id").asText(),
                "totalWeightKg",
                "10",
                "inputItems",
                List.of(
                    Map.of("materialSubtypeId", f.material(), "weightKg", "10", "volumeM3", "1"))));
    request(f, "POST", "/api/v1/donations", body, 201);
    var after = json.readTree(request(f, "GET", "/api/v1/reports/summary" + query, null, 200));
    assertThat(new BigDecimal(after.get("donationWeightKg").asText())).isEqualByComparingTo("10");
    assertThat(after.get("donationWeightKg").isTextual()).isTrue();
    assertThat(request(f, "GET", "/api/v1/reports/summary.csv" + query, null, 200))
        .contains("donation_weight_kg")
        .contains("10.0000");
    request(f, "DELETE", "/api/v1/donors/" + donor.get("id").asText(), null, 409);
    request(f, "GET", "/api/v1/reports/summary?from=2020-01-01&to=2026-01-01", null, 400);
    assertThat(
            new BigDecimal(
                json.readTree(
                        request(fixture(), "GET", "/api/v1/reports/summary" + query, null, 200))
                    .get("donationWeightKg")
                    .asText()))
        .isZero();
  }

  private String request(Fixture f, String method, String path, String body, int expected)
      throws Exception {
    var builder =
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                org.springframework.http.HttpMethod.valueOf(method), path)
            .header("Authorization", token(f))
            .header("X-Organization-Id", f.org());
    if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(body);
    return mvc.perform(builder)
        .andExpect(status().is(expected))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String postCommand(Fixture f, String path, String key, String body, int status)
      throws Exception {
    return mvc.perform(
            post("/api/v1/" + path + "s")
                .header("Authorization", token(f))
                .header("X-Organization-Id", f.org())
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().is(status))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String sorting(
      Fixture f, String weight, String volume, String rejectWeight, String rejectVolume)
      throws Exception {
    return json.writeValueAsString(
        Map.of(
            "sortingType",
            "GROSS",
            "sortedItems",
            List.of(
                Map.of(
                    "inputItemId",
                    f.input(),
                    "materialSubtypeId",
                    f.material(),
                    "weightKg",
                    weight,
                    "volumeM3",
                    volume,
                    "rejectWeightKg",
                    rejectWeight,
                    "rejectVolumeM3",
                    rejectVolume))));
  }

  private String pressing(Fixture f, UUID sorted, String weight, String initial, String end)
      throws Exception {
    return json.writeValueAsString(
        Map.of(
            "pressedBales",
            List.of(
                Map.of(
                    "sortedItemId",
                    sorted,
                    "materialSubtypeId",
                    f.material(),
                    "weightKg",
                    weight,
                    "initialVolumeM3",
                    initial,
                    "finalVolumeM3",
                    end))));
  }

  private void balance(Fixture f, String weight, String volume) {
    var balance =
        jdbc.queryForMap(
            "SELECT current_weight_kg,current_volume_m3 FROM inventory_balance WHERE"
                + " organization_id=? AND material_subtype_id=?",
            f.org(),
            f.material());
    assertThat((BigDecimal) balance.get("current_weight_kg")).isEqualByComparingTo(weight);
    assertThat((BigDecimal) balance.get("current_volume_m3")).isEqualByComparingTo(volume);
  }

  private int count(Fixture f, String table) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM " + table + " WHERE organization_id=?", Integer.class, f.org());
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

  private Fixture fixture() {
    UUID actor = UUID.randomUUID(),
        org = UUID.randomUUID(),
        category = UUID.randomUUID(),
        type = UUID.randomUUID(),
        material = UUID.randomUUID(),
        donor = UUID.randomUUID(),
        donation = UUID.randomUUID(),
        input = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users(id,email,password_hash,full_name,user_role) VALUES"
            + " (?,?,'fixture','Fixture','REPRESENTATIVE')",
        actor,
        actor + "@example.test");
    jdbc.update(
        "INSERT INTO organization(id,name,organization_type,created_by) VALUES"
            + " (?,'Fixture','Association',?)",
        org,
        actor);
    jdbc.update(
        "INSERT INTO organization_membership(organization_id,user_id,role,granted_by) VALUES"
            + " (?,?,'MEMBER',?)",
        org,
        actor,
        actor);
    jdbc.update(
        "INSERT INTO material_category(id,organization_id,name,creator_id) VALUES"
            + " (?,?,'Plastic',?)",
        category,
        org,
        actor);
    jdbc.update(
        "INSERT INTO material_type(id,organization_id,category_id,name,creator_id) VALUES"
            + " (?,?,?,'PET',?)",
        type,
        org,
        category,
        actor);
    jdbc.update(
        "INSERT INTO material_subtype(id,organization_id,type_id,name,creator_id) VALUES"
            + " (?,?,?,'Clear',?)",
        material,
        org,
        type,
        actor);
    jdbc.update(
        "INSERT INTO donor(id,organization_id,name,document,donor_type,creator_id) VALUES"
            + " (?,?,'Fixture','12345678901','PF',?)",
        donor,
        org,
        actor);
    jdbc.update(
        "INSERT INTO donation(id,organization_id,donor_id,total_weight_kg,creator_id) VALUES"
            + " (?,?,?,100,?)",
        donation,
        org,
        donor,
        actor);
    jdbc.update(
        "INSERT INTO"
            + " input_item(id,organization_id,donation_id,material_subtype_id,weight_kg,volume_m3)"
            + " VALUES (?,?,?,?,100,10)",
        input,
        org,
        donation,
        material);
    return new Fixture(actor, org, material, input);
  }
}
