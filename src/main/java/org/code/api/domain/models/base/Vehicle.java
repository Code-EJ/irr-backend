package org.code.api.domain.models.base;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.code.api.domain.common.TimeStampedEntity;
import org.hibernate.annotations.SQLRestriction;

/**
 * Organization-owned fleet vehicle retained while collection history references it.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Entity
@Table(name = "vehicle")
@SQLRestriction("is_active = true")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Vehicle extends TimeStampedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "license_plate", nullable = false, length = 20)
  private String licensePlate;

  @Column(name = "model", length = 100)
  private String model;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;
}
