package org.code.api.domain.ports;

import org.code.api.domain.models.user.Session;

/**
 * Authentication boundary for existing users; provisioning never issues another user's token.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public interface AuthPort {
  /**
   * Validates existing credentials.
   *
   * @param email login address
   * @param password supplied password
   * @return signed bearer token
   */
  String authenticate(String email, String password);

  /**
   * Resolves an active database identity and current role from a valid token.
   *
   * @param token signed bearer token
   * @return current session identity
   */
  Session getSessionDetails(String token);
}
