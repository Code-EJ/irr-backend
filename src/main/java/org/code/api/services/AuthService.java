package org.code.api.services;

import org.code.api.domain.models.user.Session;
import org.code.api.domain.ports.AuthPort;

public class AuthService implements AuthPort {

  @Override
  public String authenticate(String email, String senha) {
    throw new UnsupportedOperationException(
      "Unimplemented method 'authenticate'"
    );
  }

  @Override
  public String register(String email, String senha) {
    throw new UnsupportedOperationException("Unimplemented method 'register'");
  }

  @Override
  public String renew(String token) {
    throw new UnsupportedOperationException("Unimplemented method 'renew'");
  }

  @Override
  public Session getSessionDetails(String token) {
    throw new UnsupportedOperationException(
      "Unimplemented method 'getSessionDetails'"
    );
  }
}
