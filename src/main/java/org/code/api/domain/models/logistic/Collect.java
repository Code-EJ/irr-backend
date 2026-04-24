package org.code.api.domain.models.logistic;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.common.TimeStampedEntity;
import org.code.api.domain.models.document.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "coleta",
        indexes = {
                @Index(name = "idx_coleta_data_realizacao", columnList = "data_realizacao")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collect extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "data_realizacao", nullable = false)
    private LocalDateTime dataRealizacao;

    @Column(name = "data_chegada", nullable = false)
    private LocalDateTime dataChegada;

    @Column(name = "data_saida", nullable = false)
    private LocalDateTime dataSaida;

    @Column(name = "pesagem", nullable = false, precision = 65, scale = 30)
    private BigDecimal pesagem;

    @Column(name = "quilometragem", nullable = false, precision = 65, scale = 30)
    private BigDecimal quilometragem;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "veiculo_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "coleta_veiculo_id_fk        ",
                    foreignKeyDefinition = "FOREIGN KEY (veiculo_id) REFERENCES vehicle(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            )
    )
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "motorista_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "coleta_motorista_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (motorista_id) REFERENCES driver(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            )
    )
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "mtr_gerador_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "coleta_mtr_gerador_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (mtr_gerador_id) REFERENCES documento(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            )
    )
    private Document mtrGerador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "mtr_destinador_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "coleta_mtr_destinador_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (mtr_destinador_id) REFERENCES documento(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            )
    )
    private Document mtrDestinador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "mtr_transportador_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "coleta_mtr_transportador_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (mtr_transportador_id) REFERENCES documento(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            )
    )
    private Document mtrTransportador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "diario_coleta_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "coleta_diario_coleta_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (diario_coleta_id) REFERENCES documento(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            )
    )
    private Document diarioColeta;
}
