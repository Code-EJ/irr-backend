package org.code.api.domain.ports;

/**
 * Encryption Port boundary for the IRR application.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public interface EncryptionPort {
  String encrypt(String str);

  boolean compare(String encrypted, String str);
}
