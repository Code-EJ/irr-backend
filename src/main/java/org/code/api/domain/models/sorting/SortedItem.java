package org.code.api.domain.models.sorting;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.models.collection.InputItem;
import org.code.api.domain.models.material.MaterialSubtype;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidade JPA mapeada para a tabela {@code sorted_item}.
 * Representa um item resultante da triagem, com peso/volume do material
 * aproveitado e do rejeito.
 *
 * <p>Não estende {@link org.code.api.domain.common.TimeStampedEntity}
 * pois a tabela não possui coluna {@code creator_id}.</p>
 */
@Entity
@Table(name = "sorted_item")
@SQLRestriction("is_active = true")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SortedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sorting_id")
    private Sorting sorting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "input_item_id")
    private InputItem inputItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_subtype_id", nullable = false)
    private MaterialSubtype materialSubtype;

    @Column(name = "weight_kg", nullable = false, precision = 15, scale = 4)
    private BigDecimal weightKg;

    @Column(name = "volume_m3", nullable = false, precision = 15, scale = 4)
    private BigDecimal volumeM3;

    @Column(name = "reject_weight_kg", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal rejectWeightKg = BigDecimal.ZERO;

    @Column(name = "reject_volume_m3", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal rejectVolumeM3 = BigDecimal.ZERO;

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
