package org.code.api.domain.models.sale;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.models.document.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "venda",
        indexes = {
                @Index(name = "idx_venda_nota_fiscal", columnList = "nota_fiscal_id"),
                @Index(name = "idx_venda_mtr_gerador", columnList = "mtr_gerador_id"),
                @Index(name = "idx_venda_mtr_transportador", columnList = "mtr_transportador_id"),
                @Index(name = "idx_venda_mtr_destinador", columnList = "mtr_destinador_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "comprador", nullable = false, length = 191)
    private String comprador;

    @Column(name = "data", nullable = false)
    private LocalDateTime data;

    @Column(name = "valor", nullable = false, precision = 18, scale = 2)
    private BigDecimal valor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "nota_fiscal_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "venda_nota_fiscal_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (nota_fiscal_id) REFERENCES nota_fiscal(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            ),
            nullable = false
    )
    private SaleInvoice notaFiscal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "mtr_gerador_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "venda_mtr_gerador_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (mtr_gerador_id) REFERENCES documento(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            ),
            nullable = false
    )
    private Document mtrGerador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "mtr_transportador_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "venda_mtr_transportador_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (mtr_transportador_id) REFERENCES documento(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            ),
            nullable = false
    )
    private Document mtrTransportador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "mtr_destinador_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "venda_mtr_destinador_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (mtr_destinador_id) REFERENCES documento(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            ),
            nullable = false
    )
    private Document mtrDestinador;
}