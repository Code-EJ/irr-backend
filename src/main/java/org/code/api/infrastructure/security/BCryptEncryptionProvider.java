package org.code.api.infrastructure.security;

import org.code.api.domain.exception.AuthError;
import org.code.api.domain.ports.EncryptionPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class BCryptEncryptionProvider implements EncryptionPort {
  @Autowired
  private PasswordEncoder passwordEncoder;

  @Override
  public String encrypt(String str) {
    final byte[] passwordBytes = str.getBytes();

    if (passwordBytes.length >= 72) {
      throw new AuthError.PasswordTooLong(passwordBytes.length);
    }
   
    return passwordEncoder.encode(str);
  }

  @Override
  public boolean compare(String encrypted, String str) {
    return passwordEncoder.matches(str, encrypted);
  }
}

