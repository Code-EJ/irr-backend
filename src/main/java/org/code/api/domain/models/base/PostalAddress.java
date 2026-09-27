package org.code.api.domain.models.base;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
/**
 * Optional complete postal address owned by a donor, without a separate lifecycle.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Embeddable @lombok.Getter @lombok.Setter @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
public class PostalAddress {
    @Column(name="address_line1",length=255) private String line1;
    @Column(name="address_line2",length=255) private String line2;
    @Column(name="address_city",length=100) private String city;
    @Column(name="address_region",length=100) private String region;
    @Column(name="address_postal_code",length=20) private String postalCode;
    @Column(name="address_country_code",length=2) private String countryCode;
}
