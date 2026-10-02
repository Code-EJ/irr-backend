package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.infrastructure.development.DevelopmentAdministratorInitializer;
import org.code.api.infrastructure.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies safe development provisioning, hashing and explicit role conflict handling.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@ExtendWith(MockitoExtension.class)
class DevelopmentAdministratorInitializerTest {
  @Mock UserRepository users;
  @Mock EncryptionPort encryption;

  /** Verifies the first local administrator is persisted with a hash and the administrator role. */
  @Test
  void firstAdministratorUsesHashedCredential() {
    when(users.findByEmail("developer@irr.local")).thenReturn(Optional.empty());
    when(encryption.encrypt("development-password")).thenReturn("encoded-hash");
    initializer("development-password").run(null);
    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(users).save(saved.capture());
    assertThat(saved.getValue().getPasswordHash()).isEqualTo("encoded-hash");
    assertThat(saved.getValue().getUserRole()).isEqualTo(UserRole.ADMINISTRATOR);
  }

  /** Verifies a non-administrator cannot be silently elevated by environment configuration. */
  @Test
  void conflictingRoleIsRejected() {
    when(users.findByEmail("developer@irr.local"))
        .thenReturn(Optional.of(User.builder().userRole(UserRole.REPRESENTATIVE).build()));
    assertThatThrownBy(() -> initializer("development-password").run(null))
        .isInstanceOf(IllegalStateException.class);
    verify(users, never()).save(any());
    verifyNoInteractions(encryption);
  }

  /** Verifies unchanged credentials preserve the existing password hash on repeated startup. */
  @Test
  void unchangedPasswordIsNotRehashed() {
    User existing =
        User.builder().userRole(UserRole.ADMINISTRATOR).passwordHash("existing-hash").build();
    when(users.findByEmail("developer@irr.local")).thenReturn(Optional.of(existing));
    when(encryption.compare("existing-hash", "development-password")).thenReturn(true);
    initializer("development-password").run(null);
    verify(encryption, never()).encrypt(any());
    assertThat(existing.getPasswordHash()).isEqualTo("existing-hash");
  }

  /** Verifies example placeholders fail before any database operation. */
  @Test
  void placeholderPasswordIsRejected() {
    assertThatThrownBy(() -> initializer("replace-with-a-random-application-password").run(null))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(users, encryption);
  }

  private DevelopmentAdministratorInitializer initializer(String password) {
    return new DevelopmentAdministratorInitializer(
        users, encryption, "Development Administrator", "developer@irr.local", password);
  }
}
