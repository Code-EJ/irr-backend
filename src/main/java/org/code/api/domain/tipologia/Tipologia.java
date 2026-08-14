package org.code.api.domain.tipologia;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.code.api.utils.TimeStampedEntity;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tipologia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tipologia extends TimeStampedEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "valor", precision = 65, scale = 30)
    private BigDecimal valor;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;
}
