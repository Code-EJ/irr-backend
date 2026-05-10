package org.code.api.domain.models.donation;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.code.api.domain.common.TimeStampedEntity;
import org.code.api.domain.models.base.Attachment;
import org.code.api.domain.models.base.Donor;
import org.code.api.domain.models.collection.InputItem;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade JPA mapeada para a tabela {@code donation}.
 * Representa uma doação de materiais recicláveis feita por um {@link Donor}.
 */
@Entity
@Table(name = "donation")
@SQLRestriction("is_active = true")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Donation extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "donation_date")
    private OffsetDateTime donationDate;

    @Column(name = "total_weight_kg", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalWeightKg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = false)
    private Donor donor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proof_attachment_id")
    private Attachment proofAttachment;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "donation")
    @Builder.Default
    private List<InputItem> inputItems = new ArrayList<>();
}
