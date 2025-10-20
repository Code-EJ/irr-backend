package org.code.api.domain.models.process;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.common.TimeStampedEntity;
import org.code.api.domain.enums.SourceDestinationType;

import java.math.BigDecimal;

@Entity
@Table(
        name = "material_entrada",
        indexes = {
                @Index(name = "idx_material_entrada_subtipologia", columnList = "subtipologia_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InputMaterial extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "quantidade", nullable = false, precision = 65, scale = 30)
    private BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private SourceDestinationType tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "subtipologia_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "material_entrada_subtipologia_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (subtipologia_id) REFERENCES subtipologia(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            ),
            nullable = false
    )
    private Subtypology subtipologia;
}
