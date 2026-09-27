package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.code.api.support.PostgresIntegrationTest;
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

  /** Preserves every column/default/nullability, constraint, index, trigger and domain function. */
  @Test
  void finalBaselineMatchesVerifiedPredecessor() throws Exception {
    String query =
        new ClassPathResource("schema/final-schema-query.sql")
            .getContentAsString(StandardCharsets.UTF_8);
    List<String> expected;
    try (var input = new ClassPathResource("schema/final-schema.json").getInputStream()) {
      expected = new ObjectMapper().readValue(input, new TypeReference<List<String>>() {});
    }
    var actual =
        jdbc.queryForList(query, String.class).stream()
            .map(value -> value.replace("public.", "").replaceAll("\\s+", " ").trim())
            .sorted()
            .toList();
    assertThat(actual).containsExactlyElementsOf(expected);
  }
}
