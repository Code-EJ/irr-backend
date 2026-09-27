package org.code.api.services;

import java.nio.charset.StandardCharsets;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.dto.user.CreatePartnerRequest;
import org.code.api.dto.user.UserResponse;
import org.code.api.infrastructure.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provisions partners through a method-secured administrator boundary. Existing exact email
 * identity semantics are retained until a dedicated normalization migration.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
public class PartnerProvisioningService {
  private final UserRepository users;
  private final EncryptionPort encryption;

  /**
   * Configures partner persistence and credential hashing.
   *
   * @param users account repository
   * @param encryption password hashing adapter
   */
  public PartnerProvisioningService(UserRepository users, EncryptionPort encryption) {
    this.users = users;
    this.encryption = encryption;
  }

  /**
   * Creates a partner with a selected role and a hashed initial password.
   *
   * @param request validated partner fields
   * @return safe metadata, never a token for the created account
   */
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @Transactional
  public UserResponse create(CreatePartnerRequest request) {
    if (request.userRole() == UserRole.ADMINISTRATOR)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Administrator creation is not part of partner provisioning");
    if (request.password().getBytes(StandardCharsets.UTF_8).length >= 72)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Password must contain fewer than 72 UTF-8 bytes");
    if (users.existsByEmail(request.email()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
    try {
      User user =
          users.saveAndFlush(
              User.builder()
                  .fullName(request.fullName())
                  .email(request.email())
                  .passwordHash(encryption.encrypt(request.password()))
                  .userRole(request.userRole())
                  .build());
      return new UserResponse(
          user.getId(), user.getFullName(), user.getEmail(), user.getUserRole());
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Account conflicts with an existing record");
    }
  }
}
