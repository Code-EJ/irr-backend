package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.identity.recovery.EmailSenderPort;
import org.code.api.identity.recovery.RecoveryThrottle;
import org.code.api.infrastructure.repositories.UserRepository;
import org.code.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Exercises real PostgreSQL consumption, Redis throttling and HTTP credential revocation without
 * contacting an external mailbox or exposing sensitive MockMvc diagnostics.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@TestPropertySource(properties = "irr.recovery.base-url=http://localhost:5273/reset-password")
class PasswordRecoveryIT extends PostgresIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired UserRepository users;
  @Autowired EncryptionPort encryption;
  @Autowired ObjectMapper json;
  @MockitoBean EmailSenderPort mail;
  private final ConcurrentMap<String, String> delivered = new ConcurrentHashMap<>();
  private static final String INITIAL = "isolated-recovery-before", NEW = "isolated-recovery-after";

  /** Captures only in test memory; production uses the configured provider. */
  @BeforeEach
  void capture() {
    delivered.clear();
    when(mail.enabled()).thenReturn(true);
    doAnswer(
            invocation -> {
              delivered.put(invocation.getArgument(0), invocation.getArgument(2));
              return null;
            })
        .when(mail)
        .send(anyString(), anyString(), anyString());
  }

  private User account() {
    return users.saveAndFlush(
        User.builder()
            .email(UUID.randomUUID() + "@example.test")
            .fullName("Recovery fixture")
            .passwordHash(encryption.encrypt(INITIAL))
            .userRole(UserRole.REPRESENTATIVE)
            .build());
  }

  private int request(String email) throws Exception {
    return mvc.perform(
            post("/api/v1/session/password-recovery/request")
                .with(
                    r -> {
                      r.setRemoteAddr(UUID.randomUUID().toString());
                      return r;
                    })
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("email", email))))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private String link(User user) throws Exception {
    assertThat(request(user.getEmail())).isEqualTo(202);
    await().atMost(Duration.ofSeconds(10)).until(() -> delivered.containsKey(user.getEmail()));
    return delivered.get(user.getEmail()).split("#token=")[1].split("\\n")[0];
  }

  private int confirm(String token, String password) throws Exception {
    return mvc.perform(
            post("/api/v1/session/password-recovery/confirm")
                .with(
                    r -> {
                      r.setRemoteAddr(UUID.randomUUID().toString());
                      return r;
                    })
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("token", token, "newPassword", password))))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private String login(User user, String password, int status) throws Exception {
    var result =
        mvc.perform(
                post("/api/v1/session/authenticate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsBytes(
                            Map.of("email", user.getEmail(), "password", password))))
            .andExpect(status().is(status))
            .andReturn();
    return status == 200
        ? json.readTree(result.getResponse().getContentAsString()).get("token").asText()
        : "";
  }

  /** Existing/absent/inactive/throttled input shares 202; tokens never enter the response. */
  @Test
  void neutralRequestAndHashOnlyPersistence() throws Exception {
    User user = account(), inactive = account();
    jdbc.update("UPDATE users SET is_active=false WHERE id=?", inactive.getId());
    assertThat(request(UUID.randomUUID() + "@example.test")).isEqualTo(202);
    assertThat(request(inactive.getEmail())).isEqualTo(202);
    String token = link(user);
    assertThat(token).hasSize(43);
    assertThat(request(user.getEmail())).isEqualTo(202);
    assertThat(
            jdbc.queryForObject(
                "SELECT token_hash FROM password_recovery_token WHERE user_id=?",
                String.class,
                user.getId()))
        .isEqualTo(RecoveryThrottle.hash(token))
        .isNotEqualTo(token);
    verify(mail, never()).send(eq(inactive.getEmail()), anyString(), anyString());
  }

  /** Completion revokes old bearer sessions and rejects replay without touching new credentials. */
  @Test
  void completionChangesPasswordOnceAndRevokesSessions() throws Exception {
    User user = account();
    String old = login(user, INITIAL, 200), token = link(user);
    assertThat(confirm(token, NEW)).isEqualTo(204);
    assertThat(confirm(token, NEW + "again")).isEqualTo(400);
    mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + old))
        .andExpect(status().isUnauthorized());
    login(user, INITIAL, 401);
    login(user, NEW, 200);
    assertThat(users.findById(user.getId()).orElseThrow().getCredentialVersion()).isEqualTo(1);
  }

  /**
   * Expiry, account changes and deactivation invalidate authorization independently of Redis TTL.
   */
  @Test
  void staleTokensCannotChangeCredentials() throws Exception {
    for (String change : new String[] {"expired", "email", "generation", "inactive"}) {
      User user = account();
      String token = link(user);
      switch (change) {
        case "expired" ->
            jdbc.update(
                "UPDATE password_recovery_token SET created_at=CURRENT_TIMESTAMP-INTERVAL '2 hours',expires_at=CURRENT_TIMESTAMP-INTERVAL '1 hour' WHERE user_id=?",
                user.getId());
        case "email" ->
            jdbc.update(
                "UPDATE users SET email=? WHERE id=?",
                UUID.randomUUID() + "@example.test",
                user.getId());
        case "generation" ->
            jdbc.update(
                "UPDATE users SET credential_version=credential_version+1 WHERE id=?",
                user.getId());
        default -> jdbc.update("UPDATE users SET is_active=false WHERE id=?", user.getId());
      }
      assertThat(confirm(token, NEW)).isEqualTo(400);
      assertThat(
              jdbc.queryForObject(
                  "SELECT password_hash FROM users WHERE id=?", String.class, user.getId()))
          .isEqualTo(user.getPasswordHash());
    }
    assertThat(confirm("a".repeat(43), NEW)).isEqualTo(400);
    assertThat(confirm("invalid", NEW)).isEqualTo(400);
  }

  /** Two requests cannot consume the same authorization even with different replacement values. */
  @Test
  void concurrentConfirmationHasOneWinner() throws Exception {
    User user = account();
    String token = link(user);
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Integer> task =
          () -> {
            start.await();
            return confirm(token, NEW);
          };
      var a = pool.submit(task);
      var b = pool.submit(task);
      start.countDown();
      assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(204, 400);
    }
  }

  /** A failed provider never turns the public request into an account-existence signal. */
  @Test
  void deliveryFailureRemovesAuthorizationAndDisabledProviderIsUniform() throws Exception {
    User user = account();
    var failed = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              failed.countDown();
              throw new IllegalStateException("Synthetic provider failure");
            })
        .when(mail)
        .send(anyString(), anyString(), anyString());
    assertThat(request(user.getEmail())).isEqualTo(202);
    assertThat(failed.await(10, TimeUnit.SECONDS)).isTrue();
    await()
        .atMost(Duration.ofSeconds(5))
        .until(
            () ->
                jdbc.queryForObject(
                        "SELECT count(*) FROM password_recovery_token WHERE user_id=?",
                        Integer.class,
                        user.getId())
                    == 0);
    when(mail.enabled()).thenReturn(false);
    assertThat(request(user.getEmail())).isEqualTo(503);
    assertThat(request(UUID.randomUUID() + "@example.test")).isEqualTo(503);
  }
}
