package org.code.api.services;

import java.util.UUID;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.dto.user.ChangePasswordRequest;
import org.code.api.dto.user.ResetPasswordRequest;
import org.code.api.dto.user.UpdatePartnerRequest;
import org.code.api.dto.user.UpdateProfileRequest;
import org.code.api.dto.user.UserResponse;
import org.code.api.infrastructure.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Safe platform account reads and global administrator-controlled account lifecycle. Deactivation
 * retains audit identities and is enforced on subsequent bearer-token requests.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
@lombok.RequiredArgsConstructor
public class AccountLifecycleService {
  private final UserRepository users;
  private final AuthenticatedUserProvider actor;
  private final EncryptionPort encryption;
  private final GlobalAdministratorGuard administrators;

  /** Updates only the authenticated user's name and email, preserving administrative state. */
  @Transactional
  @PreAuthorize("isAuthenticated()")
  public UserResponse updateMe(UpdateProfileRequest request) {
    User user = locked(actor.getCurrentUserId());
    user.setFullName(request.fullName().strip());
    user.setEmail(request.email());
    return response(users.saveAndFlush(user));
  }

  /** Changes credentials atomically; all existing bearer tokens become invalid after commit. */
  @Transactional
  @PreAuthorize("isAuthenticated()")
  public void changePassword(ChangePasswordRequest request) {
    User user = locked(actor.getCurrentUserId());
    if (!encryption.compare(user.getPasswordHash(), request.currentPassword()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current password is incorrect");
    replacePassword(user, request.newPassword(), false);
  }

  /**
   * Resets an account to a temporary credential without exposing a password or impersonation token.
   */
  @Transactional
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public void resetPassword(UUID id, ResetPasswordRequest request) {
    administrators.lock();
    replacePassword(locked(id), request.temporaryPassword(), true);
  }

  private void replacePassword(User user, String password, boolean mandatory) {
    PasswordPolicy.validate(password);
    if (encryption.compare(user.getPasswordHash(), password))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a different password");
    user.setPasswordHash(encryption.encrypt(password));
    user.setCredentialVersion(Math.incrementExact(user.getCredentialVersion()));
    user.setMustChangePassword(mandatory);
    users.saveAndFlush(user);
  }

  private User locked(UUID id) {
    return users
        .lockById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
  }

  /** Returns the current active account without requiring organization selection. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public UserResponse me() {
    return response(owned(actor.getCurrentUserId()));
  }

  /** Lists active platform accounts only for administrators. */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public Page<UserResponse> list(Pageable page) {
    return users.findAll(page).map(this::response);
  }

  /** Retrieves a safe account representation for administration. */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public UserResponse get(UUID id) {
    return response(owned(id));
  }

  /** Replaces account metadata and role while preserving the last active administrator. */
  @Transactional
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public UserResponse update(UUID id, UpdatePartnerRequest request) {
    administrators.lock();
    User user = locked(id);
    if (user.getUserRole() == UserRole.ADMINISTRATOR
        && request.userRole() != UserRole.ADMINISTRATOR) administrators.requireSuccessor();
    user.setFullName(request.fullName().strip());
    user.setEmail(request.email());
    user.setUserRole(request.userRole());
    return response(users.saveAndFlush(user));
  }

  /** Deactivates an account while preserving its historical creator and membership references. */
  @Transactional
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public void deactivate(UUID id) {
    administrators.lock();
    User user = locked(id);
    if (user.getUserRole() == UserRole.ADMINISTRATOR) administrators.requireSuccessor();
    user.setIsActive(false);
    users.saveAndFlush(user);
  }

  private User owned(UUID id) {
    return users
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
  }

  private UserResponse response(User user) {
    return new UserResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getUserRole(),
        user.isMustChangePassword());
  }
}
