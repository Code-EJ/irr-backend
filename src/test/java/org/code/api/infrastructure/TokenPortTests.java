package org.code.api.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.UUID;

import org.code.api.domain.exception.AuthError;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.ports.TokenPort;
import org.code.api.infrastructure.security.JWTTokenProvider;
import org.code.api.infrastructure.security.RSAConfigProps;
import org.code.api.infrastructure.security.SecurityBeansConfig;
import org.code.api.util.RSAKeysUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TokenPortTests {
  private TokenPort tokenPort;

  public TokenPortTests() {
    RSAPrivateKey privateKey = RSAKeysUtil.getPrivateKey();
    RSAPublicKey publicKey = RSAKeysUtil.getPublicKey();

    SecurityBeansConfig securityBeans = new SecurityBeansConfig(new RSAConfigProps(publicKey, privateKey));

    this.tokenPort = new JWTTokenProvider(
      securityBeans.jwtEncoder(),
      securityBeans.jwtDecoder()
    );
  }


  @Test
  @DisplayName("Should generate a valid token")
  public void shouldGenerateValidToken() {
    Instant now = Instant.now();

    String token = tokenPort.createToken(Session.builder()
      .id(UUID.randomUUID())
      .email("test@test.com")
      .issuedAt(now.getEpochSecond())
      .expiresAt(now.plusSeconds(3600).getEpochSecond())
      .build()
    );

    assertNotNull(token);
    assertTrue(token.length() > 0);
    assertTrue(token.split("\\.").length == 3); // JWT format: header.payload.signature
  }

  @Test
  @DisplayName("Should decode token and return valid session")
  public void shouldDecodeTokenAndReturnValidSession() {
    Instant now = Instant.now();
    UUID sessionId = UUID.randomUUID();
    String email = "test@test.com";

    Session originalSession = Session.builder()
      .id(sessionId)
      .email(email)
      .build();

    String token = tokenPort.createToken(originalSession);
    Session decodedSession = tokenPort.decodeToken(token);

    assertNotNull(decodedSession);
    assertEquals(email, decodedSession.getEmail());
    assertEquals(sessionId, decodedSession.getId());
  }

  @Test
  @DisplayName("Should renew token from string and return new valid token")
  public void shouldRenewTokenFromStringAndReturnNewToken() {
    Instant now = Instant.now();

    Session originalSession = Session.builder()
      .id(UUID.randomUUID())
      .email("test@test.com")
      // .issuedAt(now.getEpochSecond())
      // .expiresAt(now.plusSeconds(3600).getEpochSecond())
      .build();

    String originalToken = tokenPort.createToken(originalSession);
    String renewedToken = tokenPort.renewToken(originalToken);

    assertNotNull(renewedToken);
    assertTrue(renewedToken.length() > 0);
    assertTrue(renewedToken.split("\\.").length == 3);
  }


  @Test
  @DisplayName("Should maintain email when renewing token from string")
  public void shouldMaintainEmailWhenRenewingTokenFromString() {
    Instant now = Instant.now();
    String email = "test@test.com";

    Session originalSession = Session.builder()
      .id(UUID.randomUUID())
      .email(email)
      .build();

    String originalToken = tokenPort.createToken(originalSession);
    String renewedToken = tokenPort.renewToken(originalToken);
    Session decodedRenewedSession = tokenPort.decodeToken(renewedToken);

    assertNotNull(decodedRenewedSession);
    assertEquals(email, decodedRenewedSession.getEmail());
  }

  @Test
  @DisplayName("Should throw InvalidToken exception when decoding invalid JWT format")
  public void shouldThrowInvalidTokenExceptionWhenDecodingInvalidFormat() {
    String invalidToken = "invalid.jwt.token";

    assertThrows(AuthError.InvalidToken.class, () -> {
      tokenPort.decodeToken(invalidToken);
    });
  }

  @Test
  @DisplayName("Should throw InvalidToken exception when decoding malformed token")
  public void shouldThrowInvalidTokenExceptionWhenDecodingMalformedToken() {
    String malformedToken = "not-a-valid-token";

    assertThrows(AuthError.InvalidToken.class, () -> {
      tokenPort.decodeToken(malformedToken);
    });
  }

  @Test
  @DisplayName("Should throw InvalidToken exception when decoding empty token")
  public void shouldThrowInvalidTokenExceptionWhenDecodingEmptyToken() {
    String emptyToken = "";

    assertThrows(AuthError.InvalidToken.class, () -> {
      tokenPort.decodeToken(emptyToken);
    });
  }

  @Test
  @DisplayName("Should throw InvalidToken exception when decoding token with invalid signature")
  public void shouldThrowInvalidTokenExceptionWhenDecodingTokenWithInvalidSignature() {
    Instant now = Instant.now();
    
    Session session = Session.builder()
      .id(UUID.randomUUID())
      .email("test@test.com")
      .issuedAt(now.getEpochSecond())
      .expiresAt(now.plusSeconds(3600).getEpochSecond())
      .build();

    String validToken = tokenPort.createToken(session);
    // Tamper with the token signature
    String[] parts = validToken.split("\\.");
    String tamperedToken = parts[0] + "." + parts[1] + ".invalid-signature";

    assertThrows(AuthError.InvalidToken.class, () -> {
      tokenPort.decodeToken(tamperedToken);
    });
  }
}