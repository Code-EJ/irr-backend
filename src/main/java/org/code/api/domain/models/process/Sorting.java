package org.code.api.domain.models.process;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.common.TimeStampedEntity;
import org.code.api.domain.enums.SortingType;
import org.code.api.domain.enums.SourceDestinationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "triagem",
        indexes = {
                @Index(name = "idx_triagem_subtipologia", columnList = "subtipologia_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sorting extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "data", nullable = false)
    private LocalDateTime data;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private SortingType tipo;

    @Column(name = "volume_total", nullable = false, precision = 65, scale = 30)
    private BigDecimal volumeTotal;

    @Column(name = "volume_rejeito", nullable = false, precision = 65, scale = 30)
    private BigDecimal volumeRejeito;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_origem", nullable = false, length = 30)
    private SourceDestinationType tipoOrigem;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_destino", nullable = false, length = 30)
    private SourceDestinationType tipoDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "subtipologia_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "triagem_subtipologia_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (subtipologia_id) REFERENCES subtipologia(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            ),
            nullable = false
    )
    private Subtypology subtipologia;
}
