package org.code.api.parties.api;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.UUID;
/**
 * Explicit HTTP contracts for organization-owned team-members.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public final class TeamMemberContract {
    private TeamMemberContract() {}
    /** Validated replacement fields; lifecycle changes use DELETE. */
    @io.swagger.v3.oas.annotations.media.Schema(name="TeamMemberRequest")
    public record Request(@NotBlank @Size(max=255) String name,
        @NotBlank @Pattern(regexp="DRIVER|HELPER|OPERATOR", message="Role must be DRIVER, HELPER or OPERATOR") String role) {}
    /** Safe response without persistence entities or account data. */
    @io.swagger.v3.oas.annotations.media.Schema(name="TeamMemberResponse")
    public record Response(UUID id,String name,String role,boolean isActive,OffsetDateTime createdAt,OffsetDateTime updatedAt) {}
}
