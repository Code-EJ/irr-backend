package org.code.api.domain.models.logistic;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.code.api.domain.common.TimeStampedEntity;

/**
 * Entidade central do domínio de Logística que representa um Veículo físico da frota.
 *
 * <p>Esta classe implementa o padrão de <b>Rich Domain Model (DDD)</b>, o que significa que
 * ela não expõe setters públicos de forma anêmica. Toda alteração de estado (como inativação
 * ou alteração de placa) é feita através de métodos de negócio que protegem as invariantes
 * da classe.
 *
 * @see org.code.api.domain.common.TimeStampedEntity
 * @implNote O campo {@code ativo} é implementado como um tipo primitivo {@code boolean} para
 * mitigar riscos de {@link NullPointerException} no motor da JVM, visto que a coluna
 * de banco de dados associada não aceita valores nulos.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@SuperBuilder
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
        @Builder.Default
    private Boolean ativo = true;

    public void deactivate() {
        this.ativo = false;
    }

    public void activate() {
        this.ativo = true;
    }

    public void update(String novaPlaca, String novoModelo, boolean novoAtivo) {
        this.placa = novaPlaca;
        this.modelo = novoModelo;
        this.ativo = novoAtivo;
    }

    public boolean isActive() {
        return ativo;
    }
}
