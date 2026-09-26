package org.code.api.infrastructure.security;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.exception.AuthError;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Adapts the validated UUID principal and server-resolved authorities to the actor port.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Component
public class SpringSecurityUserProvider implements AuthenticatedUserProvider {
    /** {@inheritDoc} */
    @Override public UUID getCurrentUserId() { return (UUID) identity().getPrincipal(); }
    /** {@inheritDoc} */
    @Override public List<UserRole> getCurrentUserRoles() {
        var authorities = identity().getAuthorities().stream().map(Object::toString).toList();
        return Arrays.stream(UserRole.values()).filter(role -> authorities.contains("ROLE_" + role.name())).toList();
    }
    private Authentication identity() {
        Authentication identity = SecurityContextHolder.getContext().getAuthentication();
        if (identity == null || !identity.isAuthenticated() || !(identity.getPrincipal() instanceof UUID))
            throw new AuthError.Unauthorized("No authenticated UUID principal is available");
        return identity;
    }
}
