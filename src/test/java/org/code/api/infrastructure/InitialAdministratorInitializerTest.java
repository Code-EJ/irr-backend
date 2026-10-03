package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.infrastructure.repositories.UserRepository;
import org.code.api.infrastructure.security.InitialAdministratorInitializer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Checks explicit bootstrap gating and restart behavior with synthetic inputs.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class InitialAdministratorInitializerTest {
  /** Existing administrator history closes bootstrap even when supplied inputs are absent. */
  @Test
  void existingAdministratorPreventsCredentialRewrite() {
    var users = mock(UserRepository.class);
    var encryption = mock(EncryptionPort.class);
    var jdbc = mock(JdbcTemplate.class);
    when(jdbc.queryForObject(
            "SELECT count(*) FROM users WHERE user_role='ADMINISTRATOR'", Long.class))
        .thenReturn(1L);
    new InitialAdministratorInitializer(users, encryption, jdbc, "", "", "").run(null);
    verify(jdbc).execute("SELECT pg_advisory_xact_lock(724991603)");
    verifyNoInteractions(users, encryption);
  }

  /** First provisioning hashes the secret and requires a user-selected replacement. */
  @Test
  void firstAdministratorIsRestrictedAndHashed() {
    var users = mock(UserRepository.class);
    var encryption = mock(EncryptionPort.class);
    var jdbc = mock(JdbcTemplate.class);
    when(jdbc.queryForObject(
            "SELECT count(*) FROM users WHERE user_role='ADMINISTRATOR'", Long.class))
        .thenReturn(0L);
    when(jdbc.queryForObject(
            "SELECT count(*) FROM users WHERE email=?", Long.class, "bootstrap@example.test"))
        .thenReturn(0L);
    when(encryption.encrypt("isolated-bootstrap-input")).thenReturn("encoded");
    new InitialAdministratorInitializer(
            users,
            encryption,
            jdbc,
            "Initial administrator",
            "bootstrap@example.test",
            "isolated-bootstrap-input")
        .run(null);
    var captured = ArgumentCaptor.forClass(User.class);
    verify(users).saveAndFlush(captured.capture());
    assertThat(captured.getValue().getPasswordHash()).isEqualTo("encoded");
    assertThat(captured.getValue().isMustChangePassword()).isTrue();
    assertThat(captured.getValue().getUserRole().name()).isEqualTo("ADMINISTRATOR");
  }

  /** An existing partner address cannot be promoted by startup configuration. */
  @Test
  void conflictingAddressFailsClosed() {
    var users = mock(UserRepository.class);
    var encryption = mock(EncryptionPort.class);
    var jdbc = mock(JdbcTemplate.class);
    when(jdbc.queryForObject(
            "SELECT count(*) FROM users WHERE user_role='ADMINISTRATOR'", Long.class))
        .thenReturn(0L);
    when(jdbc.queryForObject(
            "SELECT count(*) FROM users WHERE email=?", Long.class, "bootstrap@example.test"))
        .thenReturn(1L);
    assertThatThrownBy(
            () ->
                new InitialAdministratorInitializer(
                        users,
                        encryption,
                        jdbc,
                        "Initial administrator",
                        "bootstrap@example.test",
                        "isolated-bootstrap-input")
                    .run(null))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(users, encryption);
  }
}
