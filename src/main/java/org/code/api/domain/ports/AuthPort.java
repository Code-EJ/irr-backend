package org.code.api.domain.ports;

import org.code.api.domain.models.user.Session;

public interface AuthPort {
  public String authenticate(String email, String senha);
  public String register(String email, String senha);
  public String renew(String token);
  public Session getSessionDetails(String token);
}
