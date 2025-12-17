package org.code.api.infrastructure;

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

    System.out.println(token);
  }
}