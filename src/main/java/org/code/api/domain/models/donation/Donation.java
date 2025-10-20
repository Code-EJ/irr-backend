package org.code.api.domain.models.donation;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.common.TimeStampedEntity;
import org.code.api.domain.models.process.Subtypology;
import org.code.api.domain.models.process.Typology;

import java.math.BigDecimal;

@Entity
@Table(
        name = "doacao",
        indexes = {
                @Index(name = "idx_doacao_tipologia", columnList = "tipologia_id"),
                @Index(name = "idx_doacao_subtipologia", columnList = "subtipologia_id"),
                @Index(name = "idx_doacao_doador", columnList = "doador_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "pesagem", nullable = false, precision = 65, scale = 30)
    private BigDecimal pesagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tipologia_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "doacao_tipologia_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (tipologia_id) REFERENCES tipologia(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            ),
            nullable = false
    )
    private Typology tipologia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "subtipologia_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "doacao_subtipologia_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (subtipologia_id) REFERENCES subtipologia(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            ),
            nullable = false
    )
    private Subtypology subtipologia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "doador_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "doacao_doador_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (doador_id) REFERENCES doador(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            ),
            nullable = false
    )
    private Donator doador;
}
