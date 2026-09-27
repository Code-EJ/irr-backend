package org.code.api.infrastructure.security;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.code.api.domain.ports.OrganizationScope;
import org.code.api.organizations.application.OrganizationService;
import org.code.api.organizations.domain.MembershipRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
/**
 * Verifies the explicit organization header for every scoped use case.
 * Platform administrator status never substitutes for current organization membership.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Component("organizationScope")
public class RequestOrganizationScope implements OrganizationScope {
    private final HttpServletRequest request;
    private final OrganizationService organizations;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    /**
     * @param request current servlet request proxy
     * @param organizations current membership application boundary
     */
    public RequestOrganizationScope(HttpServletRequest request, OrganizationService organizations, org.springframework.jdbc.core.JdbcTemplate jdbc) { this.request=request; this.organizations=organizations; this.jdbc=jdbc; }
    private UUID requested() {
        String value=request.getHeader("X-Organization-Id");
        if(value==null || value.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"X-Organization-Id is required for business operations");
        try { return UUID.fromString(value); }
        catch(IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"X-Organization-Id must be a UUID"); }
    }
    /** {@inheritDoc} */
    public UUID organizationId() { UUID id=requested(); lockWritingScope(id); return organizations.requireMembership(id).organizationId(); }
    /** {@inheritDoc} */
    public boolean manager() { UUID id=requested(); lockWritingScope(id); return organizations.requireMembership(id).role()==MembershipRole.MANAGER; }
    private void lockWritingScope(UUID id) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
            && !org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            jdbc.query("SELECT id FROM organization WHERE id=? FOR UPDATE", (rs,row) -> rs.getObject(1),id);
        }
    }
}
