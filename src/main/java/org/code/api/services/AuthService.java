package org.code.api.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.code.api.domain.enums.UserType;
import org.code.api.domain.exception.AuthError;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthPort;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.domain.ports.TokenPort;
import org.code.api.infrastructure.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthService implements AuthPort {
  private TokenPort tokenPort;
  private UserRepository userRepository;
  private EncryptionPort encryptionPort;

  @Override
  public String authenticate(String email, String senha) {
    throw new UnsupportedOperationException(
      "Unimplemented method 'authenticate'"
    );
  }

  @Override
  public String register(String nome, String email, String senha, String createdBy) {
    try {
      if (userRepository.existsByEmail(email)) {
        throw new AuthError.EmailOccupied(email);
      }

      Optional<User> creatorUser = createdBy != null
        ? userRepository.findById(UUID.fromString(createdBy))
        : Optional.empty();

      if (createdBy != null && creatorUser.isEmpty()) {
        throw new AuthError.CreatorUserInvalid(createdBy);
      }

      String encryptedPassword = encryptionPort.encrypt(senha);

      LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());

      User user = userRepository.save(User.builder()
        .nome(nome)
        .senha(encryptedPassword)
        .email(email)
        .createdBy(creatorUser.orElse(null))
        .tipo(UserType.ADMINISTRADOR)
        .updatedAt(now)
        .createdAt(now)
        .build()
      );

      String token = tokenPort.createToken(Session.builder()
        .id(user.getId())
        .email(user.getEmail())
        .tipo(UserType.ADMINISTRADOR)
        .build()
      );

      return token;
    } catch (IllegalArgumentException illegalArgumentException) {
      throw new AuthError.CreatorUserInvalid(createdBy, true, illegalArgumentException);
    } catch (Exception exception) {
      throw exception;
    }
  }

  @Override
  public String register(String nome, String email, String senha) {
    return register(nome, email, senha, null);
  }

  @Override
  public String renew(String token) {
    throw new UnsupportedOperationException("Unimplemented method 'renew'");
  }

  @Override
  public Session getSessionDetails(String token) {
    throw new UnsupportedOperationException(
      "Unimplemented method 'getSessionDetails'"
    );
  }
}
