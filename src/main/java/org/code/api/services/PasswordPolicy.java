package org.code.api.services;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Shared provisioning and credential-change policy, including BCrypt's UTF-8 byte boundary.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public final class PasswordPolicy {
  private PasswordPolicy() {}

  /** Rejects invalid inputs without retaining or displaying their values. */
  public static void validate(String password) {
    if (password == null
        || password.isBlank()
        || password.length() < 8
        || password.getBytes(StandardCharsets.UTF_8).length >= 72)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Password must have at least eight characters and fewer than 72 UTF-8 bytes");
  }
}
