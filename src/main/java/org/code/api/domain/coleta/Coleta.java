package org.code.api.domain.coleta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coletas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Coleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "horario_saida", nullable = false)
    private LocalDateTime horarioSaida;

    @Column(name = "horario_chegada", nullable = false)
    private LocalDateTime horarioChegada;

    @Column(nullable = false)
    private String rota;

    @Column(name = "veiculo_id", nullable = false)
    private Long veiculoId;

    @Column(nullable = false)
    private BigDecimal quilometragem;

    @Column(name = "pesagem_kg", nullable = false)
    private BigDecimal pesagemKg;

    @Column(nullable = false)
    private Boolean ativo = true;
}