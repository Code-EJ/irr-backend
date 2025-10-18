package org.code.api.domain.models.logistic;

import jakarta.persistence.*;
import lombok.*;
import org.code.api.domain.common.TimeStampedEntity;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
@Entity
@Table(
        name = "veiculo",
        indexes = {
                @Index(name = "idx_veiculo_placa", columnList = "placa", unique = true)
        }
)
public class Vehicle extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "placa", nullable = false, length = 191, unique = true)
    private String placa;

    @Column(name = "modelo", nullable = false, length = 191)
    private String modelo;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;
}
