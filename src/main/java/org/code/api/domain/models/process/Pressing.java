package org.code.api.domain.models.process;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.common.TimeStampedEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "prensagem",
        indexes = {
                @Index(name = "idx_prensagem_data", columnList = "data")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Pressing extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "data", nullable = false)
    private LocalDateTime data;

    @Column(name = "volume_total", nullable = false, precision = 65, scale = 30)
    private BigDecimal volumeTotal;

    @Column(name = "tipo_origem", nullable = false, length = 30)
    private String tipoOrigem;

    @Column(name = "tipo_destino", nullable = false, length = 30)
    private String tipoDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "subtipologia_id",
            referencedColumnName = "id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "prensagem_subtipologia_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (subtipologia_id) REFERENCES subtipologia (id) ON DELETE RESTRICT ON UPDATE CASCADE"
            )
    )
    private Subtypology subtipologia;
}
