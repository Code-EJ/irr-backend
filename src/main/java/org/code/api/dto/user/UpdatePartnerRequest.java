package org.code.api.dto.user;
import jakarta.validation.constraints.*;
import org.code.api.domain.enums.UserRole;
/**
 * Administrator replacement fields for a partner; administrator accounts are not mutable here.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record UpdatePartnerRequest(@NotBlank @Size(max=255) String fullName,@NotBlank @Email @Size(max=255) String email,@NotNull UserRole userRole) {}
