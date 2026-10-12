package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.infrastructure.repositories.UserRepository;
import org.code.api.support.PostgresIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies the credential lifecycle through real signed tokens, HTTP security and PostgreSQL.
 * Synthetic credentials are never printed in MockMvc diagnostics.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class AccountLifecycleIT extends PostgresIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository users;
  @Autowired EncryptionPort encryption;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource dataSource;
  @Autowired JwtEncoder encoder;

  /** Concurrent changes cannot both accept the same previous password or lose a generation. */
  @Test
  void concurrentPasswordChangesHaveOneWinner() throws Exception {
    User user = account(UserRole.REPRESENTATIVE);
    String token = login(user, INITIAL);
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var tasks = new ArrayList<Future<Integer>>();
      for (int i = 0; i < 2; i++) {
        final String replacement = REPLACEMENT + i;
        tasks.add(
            executor.submit(
                () -> {
                  start.await();
                  return mvc.perform(
                          body(
                              put("/api/v1/users/me/password").header("Authorization", token),
                              Map.of("currentPassword", INITIAL, "newPassword", replacement)))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      }
      start.countDown();
      var statuses = new ArrayList<Integer>();
      for (var task : tasks) statuses.add(task.get(20, TimeUnit.SECONDS));
      assertThat(statuses).allMatch(value -> value == 204 || value == 401 || value == 403);
      assertThat(statuses.stream().filter(value -> value == 204).count()).isEqualTo(1);
      assertThat(users.findById(user.getId()).orElseThrow().getCredentialVersion()).isEqualTo(1);
    }
  }

  /** Pre-upgrade official v1 tokens stay valid at generation zero and are revoked by reset. */
  @Test
  void existingSignedTokenWithoutGenerationSupportsRollingUpgrade() throws Exception {
    User user = account(UserRole.REPRESENTATIVE), admin = account(UserRole.ADMINISTRATOR);
    var claims =
        JwtClaimsSet.builder()
            .issuer("self")
            .subject(user.getEmail())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .claim("id", user.getId().toString())
            .claim("email", user.getEmail())
            .claim("userRole", user.getUserRole().name())
            .build();
    String old = "Bearer " + encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    mvc.perform(get("/api/v1/users/me").header("Authorization", old)).andExpect(status().isOk());
    mvc.perform(
            body(
                put("/api/v1/users/" + user.getId() + "/password")
                    .header("Authorization", login(admin, INITIAL)),
                Map.of("temporaryPassword", REPLACEMENT)))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/users/me").header("Authorization", old))
        .andExpect(status().isUnauthorized());
  }

  private static final String INITIAL = "isolated-initial-password";
  private static final String REPLACEMENT = "isolated-replacement-password";

  private User account(UserRole role) {
    return users.saveAndFlush(
        User.builder()
            .fullName("Identity fixture")
            .email(UUID.randomUUID() + "@example.test")
            .passwordHash(encryption.encrypt(INITIAL))
            .userRole(role)
            .build());
  }

  private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, Object value)
      throws Exception {
    return request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(value));
  }

  private String login(User user, String password) throws Exception {
    var result =
        mvc.perform(
                body(
                    post("/api/v1/session/authenticate"),
                    Map.of("email", user.getEmail(), "password", password)))
            .andExpect(status().isOk())
            .andReturn();
    return "Bearer "
        + json.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  /** Profile input cannot assign identity, active state, platform role or organization access. */
  @Test
  void profileUsesActorAndRejectsConflictingEmail() throws Exception {
    User user = account(UserRole.REPRESENTATIVE), other = account(UserRole.CITY_HALL);
    String token = login(user, INITIAL), changed = UUID.randomUUID() + "@example.test";
    mvc.perform(
            body(
                put("/api/v1/users/me").header("Authorization", token),
                Map.of(
                    "fullName",
                    "Updated Name",
                    "email",
                    changed,
                    "userRole",
                    "ADMINISTRATOR",
                    "isActive",
                    false,
                    "id",
                    other.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("id").value(user.getId().toString()))
        .andExpect(jsonPath("userRole").value("REPRESENTATIVE"))
        .andExpect(jsonPath("passwordHash").doesNotExist());
    mvc.perform(get("/api/v1/users/me").header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("email").value(changed));
    mvc.perform(
            body(
                put("/api/v1/users/me").header("Authorization", token),
                Map.of("fullName", "Conflict", "email", other.getEmail())))
        .andExpect(status().isConflict());
    mvc.perform(
            body(
                put("/api/v1/users/me").header("Authorization", token),
                Map.of("fullName", "", "email", "invalid")))
        .andExpect(status().isBadRequest());
    assertThat(users.findById(other.getId()).orElseThrow().getEmail()).isEqualTo(other.getEmail());
    mvc.perform(get("/api/v1/users").header("Authorization", token))
        .andExpect(status().isForbidden());
  }

  /** Wrong current credentials leave sessions valid; success invalidates every previous token. */
  @Test
  void passwordChangeRevokesOldTokensAndCredentials() throws Exception {
    User user = account(UserRole.REPRESENTATIVE);
    String token = login(user, INITIAL);
    mvc.perform(
            body(
                put("/api/v1/users/me/password").header("Authorization", token),
                Map.of("currentPassword", "incorrect", "newPassword", REPLACEMENT)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/users/me").header("Authorization", token)).andExpect(status().isOk());
    for (String invalid : new String[] {"short", "é".repeat(36), INITIAL})
      mvc.perform(
              body(
                  put("/api/v1/users/me/password").header("Authorization", token),
                  Map.of("currentPassword", INITIAL, "newPassword", invalid)))
          .andExpect(status().isBadRequest());
    mvc.perform(
            body(
                put("/api/v1/users/me/password").header("Authorization", token),
                Map.of("currentPassword", INITIAL, "newPassword", REPLACEMENT)))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
    mvc.perform(get("/api/v1/users/me").header("Authorization", token))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            body(
                post("/api/v1/session/authenticate"),
                Map.of("email", user.getEmail(), "password", INITIAL)))
        .andExpect(status().isUnauthorized());
    String current = login(user, REPLACEMENT);
    mvc.perform(get("/api/v1/users/me").header("Authorization", current))
        .andExpect(status().isOk());
    User persisted = users.findById(user.getId()).orElseThrow();
    assertThat(persisted.getCredentialVersion()).isEqualTo(1);
    assertThat(encryption.compare(persisted.getPasswordHash(), REPLACEMENT)).isTrue();
  }

  /** Administrative resets require global authority for all account roles. */
  @Test
  void resetRequiresMandatoryChangeAndBlocksEveryBusinessSurface() throws Exception {
    User admin = account(UserRole.ADMINISTRATOR), user = account(UserRole.REPRESENTATIVE);
    String administrator = login(admin, INITIAL), previous = login(user, INITIAL);
    User secondAdmin = account(UserRole.ADMINISTRATOR);
    String oldAdminToken = login(secondAdmin, INITIAL);
    var input = Map.of("temporaryPassword", REPLACEMENT);
    mvc.perform(
            body(
                put("/api/v1/users/" + secondAdmin.getId() + "/password")
                    .header("Authorization", administrator),
                input))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/users/me").header("Authorization", oldAdminToken))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            body(
                put("/api/v1/users/" + user.getId() + "/password")
                    .header("Authorization", previous),
                input))
        .andExpect(status().isForbidden());
    mvc.perform(
            body(
                put("/api/v1/users/" + user.getId() + "/password")
                    .header("Authorization", administrator),
                input))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/users/me").header("Authorization", previous))
        .andExpect(status().isUnauthorized());
    String temporary = login(user, REPLACEMENT);
    mvc.perform(get("/api/v1/users/me").header("Authorization", temporary))
        .andExpect(status().isOk())
        .andExpect(jsonPath("mustChangePassword").value(true));
    for (String path : new String[] {"/api/v1/organizations", "/api/v1/vehicles", "/api/v1/users"})
      mvc.perform(get(path).header("Authorization", temporary))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("error").value("password_change_required"));
    mvc.perform(
            body(
                put("/api/v1/users/me").header("Authorization", temporary),
                Map.of("fullName", "Bypass", "email", user.getEmail())))
        .andExpect(status().isForbidden());
    mvc.perform(
            body(
                put("/api/v1/users/me/password").header("Authorization", temporary),
                Map.of("currentPassword", REPLACEMENT, "newPassword", INITIAL)))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/users/me").header("Authorization", temporary))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/users/me").header("Authorization", login(user, INITIAL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("mustChangePassword").value(false));
  }

  /** Global administrators have organization access without a fabricated membership. */
  @Test
  void administrativeMembershipDiscoveryPreservesScopeAndRevocation() throws Exception {
    User admin = account(UserRole.ADMINISTRATOR), user = account(UserRole.REPRESENTATIVE);
    String administrator = login(admin, INITIAL), partner = login(user, INITIAL);
    var created =
        mvc.perform(
                body(
                    post("/api/v1/organizations").header("Authorization", administrator),
                    Map.of("name", "Identity organization", "organizationType", "COOPERATIVE")))
            .andExpect(status().isCreated())
            .andReturn();
    String org = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
    mvc.perform(get("/api/v1/organizations").header("Authorization", administrator))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/organizations").header("Authorization", partner))
        .andExpect(status().isOk())
        .andExpect(jsonPath("totalElements").value(0));
    mvc.perform(get("/api/v1/organizations/" + org).header("Authorization", administrator))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/v1/organizations/" + org + "/membership")
                .header("Authorization", administrator))
        .andExpect(status().isNotFound());
    String member = "/api/v1/organizations/" + org + "/members/" + user.getId();
    mvc.perform(get(member).header("Authorization", administrator))
        .andExpect(status().isNotFound());
    for (String role : new String[] {"MEMBER", "MANAGER", "MANAGER"}) {
      mvc.perform(body(put(member).header("Authorization", administrator), Map.of("role", role)))
          .andExpect(status().isOk());
      mvc.perform(get(member).header("Authorization", administrator))
          .andExpect(status().isOk())
          .andExpect(jsonPath("role").value(role));
    }
    mvc.perform(get(member).header("Authorization", partner)).andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/organizations/" + org + "/membership").header("Authorization", partner))
        .andExpect(status().isOk());
    mvc.perform(delete(member).header("Authorization", administrator))
        .andExpect(status().isNoContent());
    mvc.perform(get(member).header("Authorization", administrator))
        .andExpect(status().isOk())
        .andExpect(jsonPath("active").value(false));
    mvc.perform(get("/api/v1/organizations/" + org).header("Authorization", partner))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/v1/users/" + user.getId()).header("Authorization", administrator))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/users/me").header("Authorization", partner))
        .andExpect(status().isUnauthorized());
  }

  /** A populated V1 upgrades in place, preserves hashes/roles and validates on repeated startup. */
  @Test
  void populatedBaselineUpgradesWithoutRecreatingAccounts() {
    String schema = "identity_" + UUID.randomUUID().toString().replace("-", "");
    var configuration =
        Flyway.configure()
            .dataSource(dataSource)
            .schemas(schema)
            .defaultSchema(schema)
            .locations("classpath:db/migrations");
    configuration.target("1").load().migrate();
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO "
            + schema
            + ".users(id,email,password_hash,full_name,user_role) VALUES (?,?,'existing-hash','Existing','ADMINISTRATOR')",
        id,
        id + "@example.test");
    var upgrade = configuration.target("latest").load();
    assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(3);
    var row = jdbc.queryForMap("SELECT * FROM " + schema + ".users WHERE id=?", id);
    assertThat(row.get("credential_version")).isEqualTo(0L);
    assertThat(row.get("must_change_password")).isEqualTo(false);
    assertThat(row.get("password_hash")).isEqualTo("existing-hash");
    assertThat(upgrade.migrate().migrationsExecuted).isZero();
    assertThat(upgrade.validateWithResult().validationSuccessful).isTrue();
  }
}
