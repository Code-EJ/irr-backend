package org.code.api.domain.models.base;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.code.api.domain.common.TimeStampedEntity;
import org.code.api.domain.enums.DonorType;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/**
 * Organization-owned individual or company donor with optional complete postal address.
  * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Entity
@Table(name = "donor")
@SQLRestriction("is_active = true")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Donor extends TimeStampedEntity {
    @Embedded
    private PostalAddress address;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "document", nullable = false, length = 20)
    private String document;

    @Enumerated(EnumType.STRING)
    @Column(name = "donor_type", nullable = false, length = 10)
    private DonorType donorType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
