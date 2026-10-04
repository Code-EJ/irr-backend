package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.*;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.domain.ports.TokenPort;
import org.code.api.infrastructure.repositories.UserRepository;
import org.code.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Isolates last-administrator assertions from other test fixtures using a dedicated schema.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@TestPropertySource(
    properties = {
      "spring.flyway.schemas=global_admin_tests",
      "spring.flyway.default-schema=global_admin_tests",
      "spring.jpa.properties.hibernate.default_schema=global_admin_tests",
      "spring.datasource.hikari.connection-init-sql=SET search_path TO global_admin_tests"
    })
class GlobalAdministratorIT extends PostgresIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository users;
  @Autowired TokenPort tokens;
  @Autowired EncryptionPort encryption;
  @Autowired JdbcTemplate jdbc;

  /**
   * Administrator succession permits creation/edit while concurrent removal preserves a survivor.
   */
  @Test
  void administersGlobalAccountsAndProtectsLastAdministrator() throws Exception {
    User first =
        users.saveAndFlush(
            User.builder()
                .fullName("First administrator")
                .email("first@example.invalid")
                .passwordHash(encryption.encrypt("isolated-first-password"))
                .userRole(UserRole.ADMINISTRATOR)
                .build());
    String firstToken = token(first);
    mvc.perform(delete("/api/v1/users/" + first.getId()).header("Authorization", firstToken))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/api/v1/users/" + first.getId())
                .header("Authorization", firstToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "fullName",
                            "Demoted",
                            "email",
                            first.getEmail(),
                            "userRole",
                            "CITY_HALL"))))
        .andExpect(status().isConflict());
    var result =
        mvc.perform(
                post("/api/v1/users")
                    .header("Authorization", firstToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "fullName",
                                "Second administrator",
                                "email",
                                "second@example.invalid",
                                "password",
                                "isolated-second-password",
                                "userRole",
                                "ADMINISTRATOR"))))
            .andExpect(status().isCreated())
            .andReturn();
    UUID secondId =
        UUID.fromString(
            json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    User second = users.findById(secondId).orElseThrow();
    String secondToken = token(second);
    mvc.perform(
            put("/api/v1/users/" + secondId)
                .header("Authorization", firstToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "fullName",
                            "Updated administrator",
                            "email",
                            second.getEmail(),
                            "userRole",
                            "ADMINISTRATOR"))))
        .andExpect(status().isOk());
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var tasks = new ArrayList<Future<Integer>>();
      for (var entry : Map.of(first.getId(), firstToken, secondId, secondToken).entrySet())
        tasks.add(
            executor.submit(
                () -> {
                  start.await();
                  return mvc.perform(
                          delete("/api/v1/users/" + entry.getKey())
                              .header("Authorization", entry.getValue()))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      start.countDown();
      var statuses = new ArrayList<Integer>();
      for (var task : tasks) statuses.add(task.get(20, TimeUnit.SECONDS));
      assertThat(statuses).containsExactlyInAnyOrder(204, 409);
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE is_active AND user_role='ADMINISTRATOR'",
                Long.class))
        .isEqualTo(1);
  }

  private String token(User user) {
    return "Bearer "
        + tokens.createToken(
            Session.builder()
                .id(user.getId())
                .email(user.getEmail())
                .userRole(user.getUserRole())
                .build());
  }
}
