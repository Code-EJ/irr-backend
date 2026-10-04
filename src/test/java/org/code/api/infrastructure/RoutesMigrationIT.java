package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import javax.sql.DataSource;
import org.code.api.support.PostgresIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Proves V4 upgrades populated Identity-era intake without rewriting V1 through V3 or resetting
 * data.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class RoutesMigrationIT extends PostgresIntegrationTest {
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbc;

  /**
   * Existing accounts and ad-hoc collections survive with an intentionally absent route identity.
   */
  @Test
  void upgradesPopulatedV3WithoutGuessingTerritory() {
    String schema = "routes_upgrade_" + UUID.randomUUID().toString().replace("-", "");
    var configuration =
        Flyway.configure()
            .dataSource(dataSource)
            .schemas(schema)
            .defaultSchema(schema)
            .locations("classpath:db/migrations");
    configuration.target("3").load().migrate();
    UUID actor = UUID.randomUUID(),
        org = UUID.randomUUID(),
        vehicle = UUID.randomUUID(),
        driver = UUID.randomUUID(),
        collection = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO "
            + schema
            + ".users(id,email,password_hash,full_name,user_role) VALUES (?,?,'retained-hash','Existing','ADMINISTRATOR')",
        actor,
        actor + "@example.test");
    jdbc.update(
        "INSERT INTO "
            + schema
            + ".organization(id,name,organization_type,created_by) VALUES (?,'Existing','Association',?)",
        org,
        actor);
    jdbc.update(
        "INSERT INTO "
            + schema
            + ".vehicle(id,organization_id,license_plate,creator_id) VALUES (?,?,'ABC1D23',?)",
        vehicle,
        org,
        actor);
    jdbc.update(
        "INSERT INTO "
            + schema
            + ".team_member(id,organization_id,name,role,creator_id) VALUES (?,?,'Driver','DRIVER',?)",
        driver,
        org,
        actor);
    jdbc.update(
        "INSERT INTO "
            + schema
            + ".collection(id,organization_id,realization_date,total_weight_kg,vehicle_id,driver_id,creator_id,route_description,distance_km) VALUES (?,?,CURRENT_TIMESTAMP,10,?,?,?,'Legacy free text',5.125)",
        collection,
        org,
        vehicle,
        driver,
        actor);
    var upgrade = configuration.target("latest").load();
    assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(1);
    var record = jdbc.queryForMap("SELECT * FROM " + schema + ".collection WHERE id=?", collection);
    assertThat(record.get("route_execution_id")).isNull();
    assertThat(record.get("route_description")).isEqualTo("Legacy free text");
    assertThat(record.get("distance_km").toString()).isEqualTo("5.125");
    assertThat(
            jdbc.queryForObject(
                "SELECT password_hash FROM " + schema + ".users WHERE id=?", String.class, actor))
        .isEqualTo("retained-hash");
    assertThat(upgrade.validateWithResult().validationSuccessful).isTrue();
    assertThat(upgrade.migrate().migrationsExecuted).isZero();
  }
}
