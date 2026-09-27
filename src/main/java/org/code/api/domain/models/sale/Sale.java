package org.code.api.domain.models.sale;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.code.api.domain.common.TimeStampedEntity;
import org.code.api.domain.models.base.Attachment;
import org.hibernate.annotations.SQLRestriction;

/**
 * Sale document with optimistic versioning and DRAFT, POSTED or REVERSED lifecycle.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Entity
@Table(name = "sale")
@SQLRestriction("is_active = true")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Sale extends TimeStampedEntity {
  @Column(name = "status", nullable = false)
  @Builder.Default
  private String status = "DRAFT";

  @Column(name = "currency", nullable = false, length = 3)
  @Builder.Default
  private String currency = "BRL";

  @Version
  @Column(name = "version", nullable = false)
  @Builder.Default
  private Long version = 0L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "sale_date", nullable = false)
  private OffsetDateTime saleDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "buyer_id", nullable = false)
  private Buyer buyer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "nfe_attachment_id")
  private Attachment nfeAttachment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "mtr_attachment_id")
  private Attachment mtrAttachment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cdf_attachment_id")
  private Attachment cdfAttachment;

  @Column(name = "total_value", nullable = false, precision = 15, scale = 2)
  private BigDecimal totalValue;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @OneToMany(mappedBy = "sale")
  @Builder.Default
  private List<SaleItem> saleItems = new ArrayList<>();
}
