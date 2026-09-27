package org.code.api.domain.models.inventory;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.code.api.domain.enums.OperationType;
import org.code.api.domain.models.material.MaterialSubtype;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Preserved legacy inventory history; new stock postings use stock_operation and stock_movement.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
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

  /** Verified organization ownership; unmapped legacy rows remain null. */
  @Column(name = "organization_id")
  private java.util.UUID organizationId;

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
