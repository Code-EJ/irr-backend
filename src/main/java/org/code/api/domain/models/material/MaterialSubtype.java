package org.code.api.domain.models.material;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.code.api.domain.common.TimeStampedEntity;
import org.hibernate.annotations.SQLRestriction;

/**
 * Organization-owned material subtype referenced by intake and saleable stock.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Entity
@Table(name = "material_subtype")
@SQLRestriction("is_active = true")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MaterialSubtype extends TimeStampedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Version
  @Column(name = "version", nullable = false)
  @Builder.Default
  private Long version = 0L;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "type_id", nullable = false)
  private MaterialType type;
}
