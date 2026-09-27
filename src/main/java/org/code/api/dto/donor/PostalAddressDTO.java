package org.code.api.dto.donor;

import jakarta.validation.constraints.*;
import org.code.api.domain.models.base.PostalAddress;

/**
 * A complete address when supplied; omission preserves compatibility with existing donor records.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record PostalAddressDTO(
    @NotBlank @Size(max = 255) String line1,
    @Size(max = 255) String line2,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 100) String region,
    @NotBlank @Size(max = 20) String postalCode,
    @NotBlank @Pattern(regexp = "[A-Z]{2}") String countryCode) {
  /** Maps validated address fields to the owned persistence value. */
  public PostalAddress toValue() {
    return new PostalAddress(
        line1.strip(),
        line2 == null ? null : line2.strip(),
        city.strip(),
        region.strip(),
        postalCode.strip(),
        countryCode);
  }

  /** Preserves absent addresses as null in safe API responses. */
  public static PostalAddressDTO from(PostalAddress address) {
    return address == null
        ? null
        : new PostalAddressDTO(
            address.getLine1(),
            address.getLine2(),
            address.getCity(),
            address.getRegion(),
            address.getPostalCode(),
            address.getCountryCode());
  }
}
