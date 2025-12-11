package org.code.api.infrastructure;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.ports.TokenPort;
import org.springframework.stereotype.Component;

@Component
public class JWTTokenProvider implements TokenPort {

  @Override
  public String createToken(Session session) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException(
      "Unimplemented method 'createToken'"
    );
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
