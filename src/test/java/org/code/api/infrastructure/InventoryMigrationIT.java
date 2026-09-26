package org.code.api.infrastructure;

import java.util.UUID;
import javax.sql.DataSource;
import org.code.api.support.PostgresIntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

/**
 * Verifies fresh baseline creation, incremental upgrade and transactional migration failure.
 * Example V2 migrations live only in test resources and never enter the runtime image.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class InventoryMigrationIT extends PostgresIntegrationTest {
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;

    /** Verifies an incremental migration preserves data created on the shared V1 baseline. */
    @Test void baselineUpgradesIncrementallyWithoutLosingRecords() {
        String schema = baseline();
        UUID id = user(schema);
        Flyway upgrade = flyway(schema, "classpath:db/migration-examples/valid");
        assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(upgrade.validateWithResult().validationSuccessful).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM " + schema + ".users WHERE id = ?", Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM " + schema + ".flyway_schema_history WHERE version='2' AND success", Integer.class)).isEqualTo(1);
        assertThat(upgrade.migrate().migrationsExecuted).isZero();
    }

    /** Verifies failed PostgreSQL DDL rolls back without deleting existing data or blessing a version. */
    @Test void failedIncrementalMigrationRollsBackSchemaAndPreservesRecords() {
        String schema = baseline();
        UUID id = user(schema);
        assertThatThrownBy(() -> flyway(schema, "classpath:db/migration-examples/invalid").migrate())
            .isInstanceOf(FlywayException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM " + schema + ".users WHERE id = ?", Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM " + schema + ".flyway_schema_history WHERE version='2'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema=? AND table_name='failed_migration_marker'", Integer.class, schema)).isZero();
    }

    /** Verifies the initial baseline contains all current tables and can be validated repeatedly. */
    @Test void freshBaselineCreatesCompleteCurrentSchema() {
        String schema = baseline();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema=? AND table_type='BASE TABLE' AND table_name <> 'flyway_schema_history'", Integer.class, schema)).isEqualTo(21);
        assertThat(flyway(schema).validateWithResult().validationSuccessful).isTrue();
        assertThat(flyway(schema).migrate().migrationsExecuted).isZero();
    }

    private Flyway flyway(String schema, String... additionalLocations) {
        var locations = new java.util.ArrayList<String>();
        locations.add("classpath:db/migrations");
        locations.addAll(java.util.List.of(additionalLocations));
        return Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
            .locations(locations.toArray(String[]::new)).baselineOnMigrate(false).load();
    }

    private String baseline() {
        String schema = "upgrade_" + UUID.randomUUID().toString().replace("-", "");
        flyway(schema).migrate();
        return schema;
    }

    private UUID user(String schema) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO " + schema + ".users(id,email,password_hash,full_name,user_role) VALUES (?,?,'fixture','Fixture','ADMINISTRATOR')", id, id + "@example.test");
        return id;
    }
}
