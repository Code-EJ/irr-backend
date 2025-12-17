package org.code.api.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.UUID;

import org.code.api.domain.models.user.Session;
import org.code.api.domain.ports.TokenPort;
import org.code.api.util.RSAKeysUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TokenPortTests {
  private TokenPort tokenPort;

  public TokenPortTests() {
    RSAPrivateKey privateKey = RSAKeysUtil.getPrivateKey();
    RSAPublicKey publicKey = RSAKeysUtil.getPublicKey();

    SecurityConfig securityConfig = new SecurityConfig(new RSAConfigProps(publicKey, privateKey));

    this.tokenPort = new JWTTokenProvider(
      securityConfig.jwtEncoder()
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
      .issuedAt(now.getEpochSecond())
      .expiresAt(now.plusSeconds(3600).getEpochSecond())
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
      .issuedAt(now.getEpochSecond())
      .expiresAt(now.plusSeconds(3600).getEpochSecond())
      .build();

    String originalToken = tokenPort.createToken(originalSession);
    String renewedToken = tokenPort.renewToken(originalToken);

    assertNotNull(renewedToken);
    assertTrue(renewedToken.length() > 0);
    assertTrue(renewedToken.split("\\.").length == 3);
  }

  @Test
  @DisplayName("Should renew token from session and return new valid token")
  public void shouldRenewTokenFromSessionAndReturnNewToken() {
    Instant now = Instant.now();

    Session session = Session.builder()
      .id(UUID.randomUUID())
      .email("test@test.com")
      .issuedAt(now.getEpochSecond())
      .expiresAt(now.plusSeconds(3600).getEpochSecond())
      .build();

    String renewedToken = tokenPort.renewToken(session);

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
      .issuedAt(now.getEpochSecond())
      .expiresAt(now.plusSeconds(3600).getEpochSecond())
      .build();

    String originalToken = tokenPort.createToken(originalSession);
    String renewedToken = tokenPort.renewToken(originalToken);
    Session decodedRenewedSession = tokenPort.decodeToken(renewedToken);

    assertNotNull(decodedRenewedSession);
    assertEquals(email, decodedRenewedSession.getEmail());
  }
}