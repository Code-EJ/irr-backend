package org.code.api.domain.models.inventory;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.code.api.domain.models.material.MaterialSubtype;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Constrained saleable-stock projection maintained only through the transactional stock ledger.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Entity
@Table(name = "inventory_balance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryBalance {

  /** Verified organization ownership; unmapped legacy rows remain null. */
  @Column(name = "organization_id")
  private UUID organizationId;

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
