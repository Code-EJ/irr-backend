package org.code.api.domain.models.inventory;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.models.material.MaterialSubtype;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidade JPA mapeada para a tabela {@code inventory_balance}.
 * Retrato do estoque atual (materialização) por tipo de material.
 *
 * <p>Não possui {@code is_active}, {@code created_at} ou {@code creator_id}.
 * Cada registro representa o saldo consolidado de um {@link MaterialSubtype}.</p>
 */
@Entity
@Table(name = "inventory_balance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_subtype_id", nullable = false)
    private MaterialSubtype materialSubtype;

    @Column(name = "current_weight_kg", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal currentWeightKg = BigDecimal.ZERO;

    @Column(name = "current_volume_m3", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal currentVolumeM3 = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "last_updated_at")
    private OffsetDateTime lastUpdatedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
