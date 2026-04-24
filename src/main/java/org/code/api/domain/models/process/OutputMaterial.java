package org.code.api.domain.models.process;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.common.TimeStampedEntity;
import org.code.api.domain.models.sale.SaleInvoice;

import java.math.BigDecimal;

@Entity
@Table(
        name = "material_saida",
        indexes = {
                @Index(name = "idx_material_saida_subtipologia", columnList = "subtipologia_id"),
                @Index(name = "idx_material_saida_nota_fiscal", columnList = "nota_fiscal_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutputMaterial extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "quantidade", nullable = false, precision = 65, scale = 30)
    private BigDecimal quantidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "nota_fiscal_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "material_saida_nota_fiscal_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (nota_fiscal_id) REFERENCES nota_fiscal(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            ),
            nullable = false
    )
    private SaleInvoice notaFiscal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "subtipologia_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "material_saida_subtipologia_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (subtipologia_id) REFERENCES subtipologia(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            ),
            nullable = false
    )
    private Subtypology subtipologia;
}
