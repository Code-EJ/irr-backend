package org.code.api.organizations.domain;

import java.util.UUID;

/**
 * Safe membership state retained after revocation for audit continuity.
 *
 * @param organizationId scope identity
 * @param userId member identity
 * @param role organization-local role
 * @param active whether membership is enabled
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record Membership(UUID organizationId, UUID userId, MembershipRole role, boolean active) {}
