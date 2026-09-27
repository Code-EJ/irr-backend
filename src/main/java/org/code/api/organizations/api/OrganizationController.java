package org.code.api.organizations.api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.code.api.organizations.application.OrganizationService;
import org.code.api.organizations.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
/**
 * Exposes explicit organization administration and member-scoped reads through Swagger.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequestMapping(value = "/api/organizations", produces = "application/json")
@Tag(name = "Organizations")
public class OrganizationController {
    private final OrganizationService organizations;
    /** @param organizations membership application boundary */
    public OrganizationController(OrganizationService organizations) { this.organizations = organizations; }
    /** @return the current actor's bounded page of active organizations */
    @GetMapping
    @Operation(summary = "List my organizations", description = "Returns only organizations with an active membership for the current active user. Platform administrators have no automatic membership bypass.")
    public OrganizationService.OrganizationPage list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return organizations.list(page,size); }
    /** @return the organization when visible through current membership */
    @GetMapping("/{id}")
    @Operation(summary = "Get my organization", description = "Missing, inactive and foreign organizations all return 404.")
    public Organization get(@PathVariable UUID id) { return organizations.get(id); }
    /** @return a newly created organization; no memberships are inferred */
    @PostMapping
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Organization created without implicit memberships")
    @Operation(summary = "Create an organization", description = "ADMINISTRATOR only. Creates an audit event in the same transaction. Classification is descriptive and never grants permissions. Add members explicitly afterward.")
    public ResponseEntity<Organization> create(@Valid @RequestBody CreateOrganization request) { return ResponseEntity.status(201).body(organizations.create(request.name(),request.organizationType())); }
    /** @return explicitly granted membership state */
    @PutMapping("/{id}/members/{userId}")
    @Operation(summary = "Grant organization membership", description = "ADMINISTRATOR only. Requires active organization/account, records changes in the audit log, and reactivates revoked memberships. MEMBER and MANAGER are organization-local roles; neither grants platform administration. Repeating the same active grant is idempotent.")
    public Membership grant(@PathVariable UUID id, @PathVariable UUID userId, @Valid @RequestBody GrantMembership request) { return organizations.grant(id,userId,request.role()); }
    /** @return confirmation after revocation and its audit event commit */
    @DeleteMapping("/{id}/members/{userId}")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Membership revoked; audit committed")
    @Operation(summary = "Revoke organization membership", description = "ADMINISTRATOR only. Retains the membership row and writes an audit event. Further organization reads fail immediately. Existing legacy creator-owned business APIs are not yet organization-scoped.")
    public ResponseEntity<Void> revoke(@PathVariable UUID id, @PathVariable UUID userId) { organizations.revoke(id,userId); return ResponseEntity.noContent().build(); }
    /**
     * @param name nonblank display name
     * @param organizationType descriptive classification
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
     */
    public record CreateOrganization(@NotBlank @Size(max=255) String name, @NotBlank @Size(max=50) String organizationType) {}
    /**
     * @param role explicit organization-local membership role
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
     */
    public record GrantMembership(@NotNull MembershipRole role) {}
}
