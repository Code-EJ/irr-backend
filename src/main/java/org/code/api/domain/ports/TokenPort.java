package org.code.api.domain.ports;

import org.code.api.domain.models.user.Session;

public interface TokenPort {
  String createToken(Session session);
  Session decodeToken(String token);
  String renewToken(String token);
  String renewToken(Session session);
}
