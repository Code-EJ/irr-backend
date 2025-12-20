package org.code.api.services;

import org.code.api.domain.enums.UserType;
import org.code.api.domain.exception.AuthError;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthPort;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.domain.ports.TokenPort;
import org.code.api.infrastructure.repositories.UserRepository;
import org.springframework.stereotype.Service;

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
  public String register(String nome, String email, String senha) {
    try {
      boolean userExists = userRepository.existsByEmail(email);

      if (userExists) {
        throw new AuthError.EmailOccupied(email);
      }

      String encryptedPassword = encryptionPort.encrypt(senha);

      User user = userRepository.save(User.builder()
        .nome(nome)
        .email(email)
        .senha(encryptedPassword)
        .tipo(UserType.REPRESENTANTE)
        .build()
      );
    
      String token = tokenPort.createToken(Session.builder()
        .id(user.getId())
        .email(user.getEmail())
        .build()
      );

      return token;
    } catch (Exception exception) {
      throw exception;
    }
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
