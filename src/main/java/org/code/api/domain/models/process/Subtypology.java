package org.code.api.domain.models.process;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.common.TimeStampedEntity;

import java.math.BigDecimal;

@Entity
@Table(
        name = "subtipologia",
        indexes = {
                @Index(name = "idx_subtipologia_nome", columnList = "nome")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subtypology extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome", nullable = false, length = 191)
    private String nome;

    @Column(name = "valor", nullable = false, precision = 65, scale = 30)
    private BigDecimal valor;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tipologia_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "subtipologia_tipologia_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (tipologia_id) REFERENCES typology(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            ),
            nullable = false
    )
    private Typology typology;
}
