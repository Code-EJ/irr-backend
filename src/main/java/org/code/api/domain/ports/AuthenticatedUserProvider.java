package org.code.api.domain.ports;

import java.util.List;
import java.util.UUID;
import org.code.api.domain.enums.UserRole;

/**
 * Exposes the current authenticated actor without coupling use cases to servlet state.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public interface AuthenticatedUserProvider {
    /** @return the authenticated actor's persistent identifier */
    UUID getCurrentUserId();
    /** @return the actor's current server-resolved roles */
    List<UserRole> getCurrentUserRoles();
}
