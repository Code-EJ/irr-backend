package org.code.api.infrastructure;

import org.code.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies applied migration history and database-enforced inventory constraints.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class InventoryBalanceSchemaIT extends PostgresIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    /** Verifies that migrations apply and enforce inventory constraints. */
    @Test void migrationsApplyAndEnforceInventoryConstraints() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT conname FROM pg_constraint WHERE conrelid = 'inventory_balance'::regclass", String.class))
            .contains("uq_inventory_balance_material_subtype", "ck_inventory_balance_nonnegative");
    }
}
