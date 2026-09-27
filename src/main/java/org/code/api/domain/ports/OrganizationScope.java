package org.code.api.domain.ports;
import java.util.UUID;
/**
 * Provides verified organization scope, independently of the authenticated audit actor.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public interface OrganizationScope {
    /** @return organization identity after current active membership verification */
    UUID organizationId();
    /** @return true only when current membership permits organization management */
    boolean manager();
}
