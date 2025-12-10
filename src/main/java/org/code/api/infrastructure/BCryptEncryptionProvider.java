package org.code.api.infrastructure;

import org.code.api.domain.ports.EncryptionPort;

public class BCryptEncryptionProvider implements EncryptionPort {

  @Override
  public String encrypt(String str) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'encrypt'");
  }

  @Override
  public boolean compare(String encrypted, String str) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'compare'");
  }
}
