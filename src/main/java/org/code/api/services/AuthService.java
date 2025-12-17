package org.code.api.services;

import org.code.api.domain.models.user.Session;
import org.code.api.domain.ports.AuthPort;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.domain.ports.TokenPort;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthService implements AuthPort {
  private TokenPort tokenPort;
  private EncryptionPort encryptionPort;

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
