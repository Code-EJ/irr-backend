package org.code.api.domain.ports;

import org.code.api.domain.models.user.Session;

/**
 * Porta de entrada (Inbound Port) para operações de autenticação e registro de usuários.
 */
public interface AuthPort {
    String authenticate(String email, String password);
    String register(String fullName, String email, String password);
    String renew(String token);
    Session getSessionDetails(String token);
}
