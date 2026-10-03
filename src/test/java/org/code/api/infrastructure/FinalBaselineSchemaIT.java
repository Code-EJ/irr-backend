package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.code.api.support.PostgresIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Compares the fresh final baseline with the independently captured predecessor schema.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class FinalBaselineSchemaIT extends PostgresIntegrationTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource dataSource;

  /** Preserves every column/default/nullability, constraint, index, trigger and domain function. */
  @Test
  void finalBaselineMatchesVerifiedPredecessor() throws Exception {
    String schema = "baseline_" + UUID.randomUUID().toString().replace("-", "");
    Flyway.configure()
        .dataSource(dataSource)
        .schemas(schema)
        .defaultSchema(schema)
        .locations("classpath:db/migrations")
        .target("1")
        .load()
        .migrate();
    String query =
        new ClassPathResource("schema/final-schema-query.sql")
            .getContentAsString(StandardCharsets.UTF_8)
            .replace("'public'", "'" + schema + "'");
    List<String> expected;
    try (var input = new ClassPathResource("schema/final-schema.json").getInputStream()) {
      expected = new ObjectMapper().readValue(input, new TypeReference<List<String>>() {});
    }
    var actual =
        jdbc.queryForList(query, String.class).stream()
            .map(
                value ->
                    value
                        .replace(schema + ".", "")
                        .replace("public.", "")
                        .replaceAll("\\s+", " ")
                        .trim())
            .sorted()
            .toList();
    assertThat(actual).containsExactlyElementsOf(expected);
  }
}
