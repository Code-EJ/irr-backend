package org.code.api.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InventoryBalanceSchemaTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void inventory_balance_has_version_column_for_optimistic_locking() {
        Boolean exists = jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM information_schema.columns " +
            "WHERE table_name = 'inventory_balance' AND column_name = 'version')",
            Boolean.class);
        assertThat(exists).isTrue();
    }
}
