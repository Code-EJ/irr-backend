package org.code.api.infrastructure;

import org.code.api.domain.ports.EncryptionPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptEncryptionProvider implements EncryptionPort {
  @Autowired
  private PasswordEncoder passwordEncoder;

  @Override
  public String encrypt(String str) {
    return passwordEncoder.encode(str);
  }

  @Override
  public boolean compare(String encrypted, String str) {
    return passwordEncoder.matches(str, encrypted);
  }
}

