package org.code.api.domain.ports;

import org.code.api.domain.models.user.Session;

public interface AuthPort {
  String authenticate(String email, String senha);
  String register(String nome, String email, String senha, String createdBy);
  String register(String nome, String email, String senha);
  String renew(String token);
  Session getSessionDetails(String token);
}
