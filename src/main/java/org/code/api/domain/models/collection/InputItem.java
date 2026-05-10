package org.code.api.domain.models.collection;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.models.donation.Donation;
import org.code.api.domain.models.material.MaterialSubtype;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidade JPA mapeada para a tabela {@code input_item}.
 * Representa um item de material de entrada, vinculado a uma {@link Collection} ou {@link Donation}.
 *
 * <p>Não estende {@link org.code.api.domain.common.TimeStampedEntity}
 * pois a tabela não possui coluna {@code creator_id}.</p>
 */
@Entity
@Table(name = "input_item")
@SQLRestriction("is_active = true")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InputItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id")
    private Donation donation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_subtype_id", nullable = false)
    private MaterialSubtype materialSubtype;

    @Column(name = "weight_kg", nullable = false, precision = 15, scale = 4)
    private BigDecimal weightKg;

    @Column(name = "volume_m3", nullable = false, precision = 15, scale = 4)
    private BigDecimal volumeM3;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
