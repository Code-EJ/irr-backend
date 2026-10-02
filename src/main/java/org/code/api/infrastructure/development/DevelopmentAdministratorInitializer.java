package org.code.api.infrastructure.development;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.infrastructure.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisions the explicit local administrator from development environment settings. This component
 * cannot run without both the development profile and bootstrap flag. It never logs credentials or
 * promotes an existing non-administrator account.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Component
@org.springframework.core.annotation.Order(0)
@Profile("development")
@ConditionalOnProperty(name = "irr.development.bootstrap-enabled", havingValue = "true")
public class DevelopmentAdministratorInitializer implements ApplicationRunner {
  private final UserRepository users;
  private final EncryptionPort encryption;
  private final String name;
  private final String email;
  private final String password;

  /**
   * Configures the local administrator without reading a committed credential.
   *
   * @param users user persistence adapter
   * @param encryption password hashing adapter
   * @param name administrator display name
   * @param email administrator login address
   * @param password development-only plaintext input, persisted only as a hash
   */
  public DevelopmentAdministratorInitializer(
      UserRepository users,
      EncryptionPort encryption,
      @Value("${irr.development.admin-name}") String name,
      @Value("${irr.development.admin-email}") String email,
      @Value("${irr.development.admin-password}") String password) {
    this.users = users;
    this.encryption = encryption;
    this.name = name;
    this.email = email.trim().toLowerCase(Locale.ROOT);
    this.password = password;
  }

  /**
   * Creates or synchronizes the configured development administrator on startup. A changed
   * development password is rehashed; an unchanged hash is retained.
   *
   * @param arguments application startup arguments (unused)
   * @throws IllegalStateException if configuration is invalid or an existing role conflicts
   */
  @Override
  @Transactional
  public void run(ApplicationArguments arguments) {
    if (name.isBlank()
        || name.length() > 255
        || email.length() > 255
        || email.chars().anyMatch(Character::isWhitespace)
        || !email.matches("[^ @]+@[^ @]+[.][^ @]+")
        || password.length() < 12
        || password.getBytes(StandardCharsets.UTF_8).length > 72
        || password.startsWith("replace-with-")) {
      throw new IllegalStateException(
          "Configure a valid development administrator and a non-placeholder password of 12-72"
              + " UTF-8 bytes");
    }
    User user =
        users
            .findByEmail(email)
            .orElseGet(
                () ->
                    User.builder()
                        .email(email)
                        .userRole(UserRole.ADMINISTRATOR)
                        .isActive(true)
                        .build());
    if (user.getUserRole() != UserRole.ADMINISTRATOR) {
      throw new IllegalStateException(
          "The development bootstrap email belongs to a non-administrator");
    }
    user.setFullName(name);
    if (user.getPasswordHash() == null || !encryption.compare(user.getPasswordHash(), password)) {
      user.setPasswordHash(encryption.encrypt(password));
    }
    users.save(user);
  }
}
