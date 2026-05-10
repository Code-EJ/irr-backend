package org.code.api.domain.models.inventory;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.enums.OperationType;
import org.code.api.domain.models.material.MaterialSubtype;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidade JPA mapeada para a tabela {@code inventory_log}.
 * Registra cada movimentação de estoque (entrada/saída) por tipo de material.
 *
 * <p>Não estende {@link org.code.api.domain.common.TimeStampedEntity}
 * pois a tabela não possui coluna {@code creator_id}.</p>
 */
@Entity
@Table(name = "inventory_log")
@SQLRestriction("is_active = true")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_subtype_id", nullable = false)
    private MaterialSubtype materialSubtype;

    @Column(name = "quantity_kg", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantityKg;

    @Column(name = "quantity_m3", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantityM3;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 50)
    private OperationType operationType;

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
