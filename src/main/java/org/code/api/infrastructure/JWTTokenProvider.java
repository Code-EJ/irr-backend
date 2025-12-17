package org.code.api.infrastructure;

import java.time.Instant;

import org.code.api.domain.exception.AuthError;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.ports.TokenPort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class JWTTokenProvider implements TokenPort {
  private JwtEncoder jwtEncoder;
  private JwtDecoder jwtDecoder;

  @Override
  public String createToken(Session session) {
    try {
      Instant now = Instant.now();

      JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("self")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(60*60*72))
        .subject(session.getEmail())
        .claim("session", session)
        .build();

      String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

      return token;
    } catch (JwtEncodingException exception) {
      log.error("Error encoding JWT token: {}", exception.getMessage());
      throw new AuthError.TokenCreationError("Error creating JWT token", exception);
    } catch (Exception exception) {
      throw exception;
    }
  }

  @Override
  public Session decodeToken(String token) {
    try {
      Jwt jwt = jwtDecoder.decode(token);
      Session session = jwt.getClaim("session");

      return session;
    } catch (JwtException jwtException) {
      log.error("Invalid JWT token: {}", jwtException.getMessage());
      throw new AuthError.InvalidToken(token, jwtException);
    } catch (Exception e) {
      throw e;
    }
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
