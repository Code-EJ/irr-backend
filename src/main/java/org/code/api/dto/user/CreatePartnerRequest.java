package org.code.api.dto.user;

import jakarta.validation.constraints.*;
import org.code.api.domain.enums.UserRole;

/**
 * Administrator input for partner provisioning; passwords are never returned.
 * @param fullName display name
 * @param email unique login address
 * @param password initial partner password
 * @param userRole one of the supported partner roles
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record CreatePartnerRequest(
    @NotBlank @Size(max=255) String fullName,
    @NotBlank @Email @Size(max=255) String email,
    @NotBlank @Size(min=8,max=72) @io.swagger.v3.oas.annotations.media.Schema(accessMode=io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY) String password,
    @NotNull @io.swagger.v3.oas.annotations.media.Schema(allowableValues={"REPRESENTATIVE","ORGANIZATION","CITY_HALL"}) UserRole userRole
) {}
