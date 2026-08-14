package org.code.api.domain.prensagem;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.code.api.domain.subtipologia.Subtipologia;
import org.code.api.utils.TimeStampedEntity;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prensagem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Prensagem extends TimeStampedEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "data", nullable = false)
    private LocalDateTime data;

    @Column(name = "volume_total", nullable = false, precision = 65, scale = 30)
    private BigDecimal volumeTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_origem", nullable = false, length = 30)
    private TipoOrigemDestino tipoOrigem;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_destino", nullable = false, length = 30)
    private TipoOrigemDestino tipoDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subtipologia_id", nullable = false)
    private Subtipologia subtipologia;
}
