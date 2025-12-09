package org.code.api.domain.ports;

import jakarta.websocket.Session;

public interface AuthPort {
  public String authenticate(String email, String senha);
  public String register(String email, String senha);
  public String renew(String token);
  public Session getSessionDetails(String token);
}
