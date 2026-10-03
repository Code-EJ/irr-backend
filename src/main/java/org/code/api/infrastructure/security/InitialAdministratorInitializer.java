package org.code.api.infrastructure.security;

import org.code.api.domain.enums.UserRole;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.infrastructure.repositories.UserRepository;
import org.code.api.services.PasswordPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Explicit first-deployment bootstrap, independent of development and incapable of resetting
 * accounts. The transaction lock serializes parallel startup; existing administrator history closes
 * bootstrap.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Component
@Profile("!development")
@ConditionalOnProperty(name = "irr.initial-admin.enabled", havingValue = "true")
public class InitialAdministratorInitializer implements ApplicationRunner {
  private final UserRepository users;
  private final EncryptionPort encryption;
  private final JdbcTemplate jdbc;
  private final String name;
  private final String email;
  private final String password;

  /** Binds optional operator-supplied bootstrap inputs without logging or persisting plaintext. */
  public InitialAdministratorInitializer(
      UserRepository users,
      EncryptionPort encryption,
      JdbcTemplate jdbc,
      @Value("${irr.initial-admin.name}") String name,
      @Value("${irr.initial-admin.email}") String email,
      @Value("${irr.initial-admin.password}") String password) {
    this.users = users;
    this.encryption = encryption;
    this.jdbc = jdbc;
    this.name = name;
    this.email = email;
    this.password = password;
  }

  /**
   * Creates only the first administrator; restart never recreates, promotes or resets an account.
   */
  @Override
  @Transactional
  public void run(ApplicationArguments arguments) {
    jdbc.execute("SELECT pg_advisory_xact_lock(724991603)");
    if (jdbc.queryForObject(
            "SELECT count(*) FROM users WHERE user_role='ADMINISTRATOR'", Long.class)
        > 0) return;
    if (name.isBlank()
        || name.length() > 255
        || email.length() > 255
        || !email.matches("[^\\s@]+@[^\\s@]+[.][^\\s@]+"))
      throw new IllegalStateException("Configure valid initial administrator metadata");
    PasswordPolicy.validate(password);
    if (jdbc.queryForObject("SELECT count(*) FROM users WHERE email=?", Long.class, email) > 0)
      throw new IllegalStateException(
          "Initial administrator address already belongs to an account");
    users.saveAndFlush(
        User.builder()
            .fullName(name.strip())
            .email(email)
            .passwordHash(encryption.encrypt(password))
            .userRole(UserRole.ADMINISTRATOR)
            .mustChangePassword(true)
            .build());
  }
}
