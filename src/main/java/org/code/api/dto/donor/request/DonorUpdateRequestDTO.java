package org.code.api.dto.donor.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.code.api.dto.donor.PostalAddressDTO;

/**
 * Typed Donor Update Request API value.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record DonorUpdateRequestDTO(
    @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,
    @Size(max = 20, message = "Document must be at most 20 characters") String document,
    @jakarta.validation.Valid PostalAddressDTO address) {}
