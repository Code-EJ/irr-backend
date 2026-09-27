package org.code.api.organizations.application;
import java.util.List;
import java.util.UUID;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.organizations.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Coordinates explicit organization membership without inferring ownership from creator fields.
 * Membership checks query PostgreSQL on every call; Redis is not an authorization cache.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
@PreAuthorize("isAuthenticated()")
public class OrganizationService {
    private final OrganizationStore store;
    private final AuthenticatedUserProvider actor;
    /**
     * @param store organization persistence boundary
     * @param actor authenticated server identity
     */
    public OrganizationService(OrganizationStore store, AuthenticatedUserProvider actor) { this.store = store; this.actor = actor; }
    /**
     * Lists current memberships without an implicit administrator bypass.
     * @param page zero-based page
     * @param size page size, from one to one hundred
     * @return visible organizations and total count
     */
    @Transactional(readOnly = true)
    public OrganizationPage list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Page must be nonnegative and size must be between 1 and 100");
        UUID id = actor.getCurrentUserId();
        return new OrganizationPage(store.list(id, size, (long) page * size), page, size, store.count(id));
    }
    /**
     * Reads one organization only when an active association grants access.
     * @param id requested organization
     * @return visible organization
     */
    @Transactional(readOnly = true)
    public Organization get(UUID id) { return store.accessible(id, actor.getCurrentUserId()).orElseThrow(OrganizationAccessDenied::new); }
    /**
     * Resolves current scope for future organization-owned use cases.
     * @param id requested scope; never trusted without membership verification
     * @return active membership with its organization-local role
     */
    @Transactional(readOnly = true)
    public Membership requireMembership(UUID id) { return store.access(id, actor.getCurrentUserId()).orElseThrow(OrganizationAccessDenied::new); }
    /**
     * Creates an organization and an audit event without automatically associating any user.
     * @param name display name
     * @param type descriptive classification, not a security role
     * @return new organization
     */
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Organization create(String name, String type) {
        UUID actorId = actor.getCurrentUserId(), id = UUID.randomUUID();
        Organization result = store.create(id, name.trim(), type.trim(), actorId);
        store.audit(id, actorId, null, "CREATE", null, null);
        return result;
    }
    /**
     * Explicitly grants or changes membership, serializing competing administration requests.
     * @param organization requested scope
     * @param user target active account
     * @param role organization-local role
     * @return current membership
     */
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Membership grant(UUID organization, UUID user, MembershipRole role) {
        store.lock(organization);
        if (!store.activeUser(user)) throw new OrganizationAccessDenied();
        var previous = store.membership(organization, user);
        if (previous.isPresent() && previous.get().active() && previous.get().role() == role) return previous.get();
        store.grant(organization, user, role, actor.getCurrentUserId());
        store.audit(organization, actor.getCurrentUserId(), user, "GRANT", previous.map(m -> m.role().name()).orElse(null), role.name());
        return new Membership(organization, user, role, true);
    }
    /**
     * Revokes access without deleting membership history; an already revoked association is unchanged.
     * @param organization requested scope
     * @param user target member
     */
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void revoke(UUID organization, UUID user) {
        store.lock(organization);
        Membership previous = store.membership(organization, user).orElseThrow(OrganizationAccessDenied::new);
        if (!previous.active()) return;
        store.revoke(organization, user);
        store.audit(organization, actor.getCurrentUserId(), user, "REVOKE", previous.role().name(), null);
    }
    /**
     * Bounded organization page without persistence internals.
     * @param content visible organizations
     * @param page requested page
     * @param size requested size
     * @param totalElements visible organization count
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
     */
    public record OrganizationPage(List<Organization> content, int page, int size, long totalElements) {}
}
