package org.code.api.domain.models.donation;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.common.TimeStampedEntity;
import org.code.api.domain.enums.DonatorType;

@Entity
@Table(
        name = "doador",
        indexes = {
                @Index(name = "idx_doador_cadastro_nacional", columnList = "cadastro_nacional", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donator extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome", nullable = false, length = 191)
    private String nome;

    @Column(name = "endereco", nullable = false, length = 191)
    private String endereco;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private DonatorType tipo;

    @Column(name = "cadastro_nacional", nullable = false, length = 191, unique = true)
    private String cadastroNacional;
}
