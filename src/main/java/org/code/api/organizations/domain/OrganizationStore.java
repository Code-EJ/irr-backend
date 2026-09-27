package org.code.api.organizations.domain;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
/**
 * Persistence boundary for organization administration and current membership resolution.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public interface OrganizationStore {
    /** @return active organizations visible to the supplied user */
    List<Organization> list(UUID actor, int limit, long offset);
    /** @return the number of active organizations visible to the supplied user */
    long count(UUID actor);
    /** @return the organization when active and visible to the actor */
    Optional<Organization> accessible(UUID organization, UUID actor);
    /** @return the active membership after also checking account and organization status */
    Optional<Membership> access(UUID organization, UUID actor);
    /** @return the newly persisted organization */
    Organization create(UUID id, String name, String type, UUID actor);
    /** Locks an active organization until the surrounding transaction ends. */
    void lock(UUID organization);
    /** @return whether the target account is currently active */
    boolean activeUser(UUID user);
    /** @return membership state including a previously revoked association */
    Optional<Membership> membership(UUID organization, UUID user);
    /** Inserts or reactivates an explicit association in the current transaction. */
    void grant(UUID organization, UUID user, MembershipRole role, UUID actor);
    /** Revokes an association without deleting its historical identity. */
    void revoke(UUID organization, UUID user);
    /** Records an administrator action in the same transaction as its effect. */
    void audit(UUID organization, UUID actor, UUID subject, String action, String before, String after);
}
