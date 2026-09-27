package org.code.api.dto.donor.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.code.api.domain.enums.DonorType;

/**
 * Typed Donor Create Request API value.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record DonorCreateRequestDTO(
    @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,
    @NotBlank(message = "Document is required")
        @Size(max = 20, message = "Document must be at most 20 characters")
        String document,
    @NotNull(message = "Donor type is required") DonorType donorType,
    @jakarta.validation.Valid org.code.api.dto.donor.PostalAddressDTO address) {}
