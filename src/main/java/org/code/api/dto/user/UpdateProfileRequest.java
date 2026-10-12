package org.code.api.dto.user;

import jakarta.validation.constraints.*;

/**
 * Self-service fields; no administrative attributes are accepted or assigned.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record UpdateProfileRequest(
    @NotBlank @Size(max = 255) String fullName, @NotBlank @Email @Size(max = 255) String email) {}
