package org.code.api.domain.ports;

public interface EncryptionPort {
  String encrypt(String str);
  boolean compare(String encrypted, String str);
}
