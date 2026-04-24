package org.code.api.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.code.api.domain.exception.AuthError;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.infrastructure.security.BCryptEncryptionProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class EncryptionPortTests {
  private final EncryptionPort encryptionPort;

  public EncryptionPortTests() {
    encryptionPort = new BCryptEncryptionProvider(new BCryptPasswordEncoder());
  }

  @Test
  @DisplayName("Expect to return an password different of what it was given as parameter")
  public void testPasswordEncoding() {
    String password = "P@ssw0rd!";
    String encrypted = encryptionPort.encrypt(password);

    assertFalse(password.equals(encrypted), "Encrypted password should be different from unencrypted password");
  }

  @Test
  @DisplayName("Expect to return a non-null encrypted password")
  public void testEncryptReturnsNonNull() {
    String password = "testPassword123";
    String encrypted = encryptionPort.encrypt(password);

    assertNotNull(encrypted, "Encrypted password should not be null");
  }

  @Test
  @DisplayName("Expect the same password to generate different hashes")
  public void testSamePasswordGeneratesDifferentHashes() {
    String password = "samePassword";
    String encrypted1 = encryptionPort.encrypt(password);
    String encrypted2 = encryptionPort.encrypt(password);

    assertNotEquals(encrypted1, encrypted2, "Same password should generate different hashes due to salt");
  }

  @Test
  @DisplayName("Expect compare to return true for matching password")
  public void testCompareWithCorrectPassword() {
    String password = "mySecurePassword";
    String encrypted = encryptionPort.encrypt(password);

    assertTrue(encryptionPort.compare(encrypted, password), "Compare should return true for matching password");
  }

  @Test
  @DisplayName("Expect compare to return false for non-matching password")
  public void testCompareWithIncorrectPassword() {
    String password = "correctPassword";
    String wrongPassword = "wrongPassword";
    String encrypted = encryptionPort.encrypt(password);

    assertFalse(encryptionPort.compare(encrypted, wrongPassword), "Compare should return false for non-matching password");
  }

  @Test
  @DisplayName("Expect to handle empty string encryption")
  public void testEncryptEmptyString() {
    String emptyPassword = "";
    String encrypted = encryptionPort.encrypt(emptyPassword);

    assertNotNull(encrypted, "Encrypted empty string should not be null");
    assertTrue(encryptionPort.compare(encrypted, emptyPassword), "Compare should work with empty string");
  }

  @Test
  @DisplayName("Expect to handle special characters in password")
  public void testEncryptWithSpecialCharacters() {
    String password = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
    String encrypted = encryptionPort.encrypt(password);

    assertNotNull(encrypted, "Encrypted password with special characters should not be null");
    assertTrue(encryptionPort.compare(encrypted, password), "Compare should work with special characters");
  }

  @Test
  @DisplayName("Expect to handle long passwords")
  public void testEncryptLongPassword() {
    String longPassword = "a".repeat(100);
    
    try {
      encryptionPort.encrypt(longPassword);
      fail("Expected to receive an password too long exception.");
    } catch (AuthError.PasswordTooLong ignored) {
    } catch (Exception e) {
      fail("Expected AuthError.PasswordTooLong exception, but got: " + e.getClass().getName());
    }
  }

  @Test
  @DisplayName("Expect compare to be case sensitive")
  public void testCompareCaseSensitive() {
    String password = "Password123";
    String encrypted = encryptionPort.encrypt(password);

    assertFalse(encryptionPort.compare(encrypted, "password123"), "Compare should be case sensitive");
    assertFalse(encryptionPort.compare(encrypted, "PASSWORD123"), "Compare should be case sensitive");
  }

  @Test
  @DisplayName("Expect to handle unicode characters")
  public void testEncryptWithUnicodeCharacters() {
    String password = "senha123çãéô你好مرحبا";
    String encrypted = encryptionPort.encrypt(password);

    assertNotNull(encrypted, "Encrypted password with unicode should not be null");
    assertTrue(encryptionPort.compare(encrypted, password), "Compare should work with unicode characters");
  }
}
