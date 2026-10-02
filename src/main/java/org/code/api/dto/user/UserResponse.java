package org.code.api.dto.user;

import java.util.UUID;
import org.code.api.domain.enums.UserRole;

/**
 * Safe account metadata without a password hash or an impersonation token.
 *
 * @param id persistent account identifier
 * @param fullName display name
 * @param email login address
 * @param userRole assigned role
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record UserResponse(UUID id, String fullName, String email, UserRole userRole) {}
