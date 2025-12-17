package org.code.api.infrastructure;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
        .expiresAt(now.plusSeconds(60*60*72*1000))
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
      Map<String, Object> session = jwt.getClaim("session");

      return Session.builder()
        .id(UUID.fromString((String) session.get("id")))
        .email((String) session.get("email"))
        .issuedAt(((Number) session.get("issuedAt")).longValue())
        .expiresAt(((Number) session.get("expiresAt")).longValue())
        .build();
    } catch (JwtException jwtException) {
      log.error("Invalid JWT token: {}", jwtException.getMessage());
      throw new AuthError.InvalidToken(token, jwtException);
    } catch (Exception e) {
      throw e;
    }
  }

  @Override
  public String renewToken(String token) {
    try {
      Session session = decodeToken(token);
      return createToken(session);
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Por enquanto botei um tempo de 24 horas após a expiração do token pra fazer a renovação.
   * Mas isso é algo que deve ser discutido melhor depois (se já não foi decidido também).
   */
  @Override
  public String renewToken(Session session) {
    try {
      if (session.isOnRenewalGrace()) {
        return createToken(session);
      }
      
      throw new AuthError.ExpiredToken(session, session.getExpiresAt(), session.getIssuedAt());
    } catch (Exception e) {
      throw e;  
    }
  }
}
