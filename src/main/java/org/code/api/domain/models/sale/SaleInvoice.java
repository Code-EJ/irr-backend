package org.code.api.domain.models.sale;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.common.TimeStampedEntity;
import org.code.api.domain.models.logistic.Collect;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nota_fiscal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleInvoice extends TimeStampedEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "numero", nullable = false, length = 50)
    private String numero;

    @Column(name = "data_emissao", nullable = false)
    private LocalDateTime dataEmissao;

    @Column(name = "valor_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "descricao")
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "coleta_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "nota_fiscal_coleta_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (coleta_id) REFERENCES coleta(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            ),
            nullable = false
    )
    private Collect coleta;
}
