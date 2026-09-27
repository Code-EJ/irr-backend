package org.code.api.services;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.exception.AuthError;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthPort;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.domain.ports.TokenPort;
import org.code.api.infrastructure.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authenticates existing active users and reloads current identity/roles for bearer sessions. User
 * provisioning is a separate administrator-only use case.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Slf4j
@Service
@AllArgsConstructor
public class AuthService implements AuthPort {

  private TokenPort tokenPort;
  private UserRepository userRepository;
  private EncryptionPort encryptionPort;

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public String authenticate(String email, String password) {
    Optional<User> userOptional = userRepository.findByEmail(email);

    if (userOptional.isEmpty()) {
      throw new AuthError.WrongCredentials(email, false);
    }

    User user = userOptional.get();

    if (!encryptionPort.compare(user.getPasswordHash(), password)) {
      throw new AuthError.WrongCredentials(email, true);
    }

    return tokenPort.createToken(
        Session.builder()
            .id(user.getId())
            .email(user.getEmail())
            .userRole(user.getUserRole())
            .build());
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public Session getSessionDetails(String token) {
    Session session = tokenPort.decodeToken(token);
    Optional<User> userOptional = userRepository.findById(session.getId());

    log.debug(
        "Decoded session token:\nUser email: {}\nExpiresAt: {}, IssuedAt: {}",
        userOptional.map(User::getEmail).orElse("UNKNOWN"),
        session.getExpiresAt(),
        session.getIssuedAt());

    return userOptional
        .map(
            user ->
                Session.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .userRole(user.getUserRole())
                    .issuedAt(session.getIssuedAt())
                    .expiresAt(session.getExpiresAt())
                    .build())
        .orElseThrow(() -> new AuthError.InvalidToken(token));
  }
}
