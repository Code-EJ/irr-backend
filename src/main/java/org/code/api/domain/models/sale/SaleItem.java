package org.code.api.domain.models.sale;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.code.api.domain.models.material.MaterialSubtype;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Draft or historical sale allocation to one organization-owned stock lot.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Entity
@Table(name = "sale_item")
@SQLRestriction("is_active = true")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleItem {
  @Column(name = "stock_lot_id")
  private UUID stockLotId;

  /** Verified organization ownership; unmapped legacy rows remain null. */
  @Column(name = "organization_id")
  private java.util.UUID organizationId;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sale_id")
  private Sale sale;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "material_subtype_id", nullable = false)
  private MaterialSubtype materialSubtype;

  @Column(name = "weight_kg", nullable = false, precision = 15, scale = 4)
  private BigDecimal weightKg;

  @Column(name = "volume_m3", nullable = false, precision = 15, scale = 4)
  private BigDecimal volumeM3;

  @Column(name = "unit_price", nullable = false, precision = 15, scale = 4)
  private BigDecimal unitPrice;

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
