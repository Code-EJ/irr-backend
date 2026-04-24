package org.code.api.domain.models.logistic;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.common.TimeStampedEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "motorista",
        indexes = {
                @Index(name = "idx_motorista_cpf", columnList = "cpf", unique = true)
        }
)
public class Driver extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "nome", nullable = false, length = 191)
    private String nome;

    @Column(name = "cpf", nullable = false, length = 191, unique = true)
    private String cpf;
}
