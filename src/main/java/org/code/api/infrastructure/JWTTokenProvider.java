package org.code.api.infrastructure;

import java.time.Instant;

import org.code.api.domain.models.user.Session;
import org.code.api.domain.ports.TokenPort;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class JWTTokenProvider implements TokenPort {
  private JwtEncoder jwtEncoder;

  @Override
  public String createToken(Session session) {
    Instant now = Instant.now();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
      .keyId("irr-hmac-key")
      .build();

    JwtClaimsSet claims = JwtClaimsSet.builder()
      .issuer("self")
      .issuedAt(now)
      .expiresAt(now.plusSeconds(60*60*72))
      .subject(session.getEmail())
      .claim("session", session)
      .build();

    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

    return token;
  }

  @Override
  public Session decodeToken(String token) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException(
      "Unimplemented method 'decodeToken'"
    );
  }

  @Override
  public String renewToken(String token) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException(
      "Unimplemented method 'renewToken'"
    );
  }

  @Override
  public String renewToken(Session session) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException(
      "Unimplemented method 'renewToken'"
    );
  }
}
