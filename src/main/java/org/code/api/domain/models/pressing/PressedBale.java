package org.code.api.domain.models.pressing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.code.api.domain.enums.DestinationType;
import org.code.api.domain.models.material.MaterialSubtype;
import org.code.api.domain.models.sorting.SortedItem;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Pressed output retaining its sorted source and initial/final volume.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Entity
@Table(name = "pressed_bale")
@SQLRestriction("is_active = true")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PressedBale {

  /** Verified organization ownership; unmapped legacy rows remain null. */
  @Column(name = "organization_id")
  private UUID organizationId;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pressing_id")
  private Pressing pressing;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sorted_item_id")
  private SortedItem sortedItem;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "material_subtype_id", nullable = false)
  private MaterialSubtype materialSubtype;

  @Column(name = "weight_kg", nullable = false, precision = 15, scale = 4)
  private BigDecimal weightKg;

  @Column(name = "initial_volume_m3", nullable = false, precision = 15, scale = 4)
  private BigDecimal initialVolumeM3;

  @Column(name = "final_volume_m3", nullable = false, precision = 15, scale = 4)
  private BigDecimal finalVolumeM3;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "destination_type", length = 50)
  private DestinationType destinationType;

  @Column(name = "destination_id")
  private UUID destinationId;
}
