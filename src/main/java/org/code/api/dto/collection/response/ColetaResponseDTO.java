package org.code.api.dto.collection.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.code.api.domain.coleta.Coleta;

public record ColetaResponseDTO(
        Long id,
        LocalDateTime horarioSaida,
        LocalDateTime horarioChegada,
        String rota,
        Long veiculoId,
        BigDecimal quilometragem,
        BigDecimal pesagemKg,
        Boolean ativo
) {
    public ColetaResponseDTO(Coleta coleta) {
        this(
                coleta.getId(),
                coleta.getHorarioSaida(),
                coleta.getHorarioChegada(),
                coleta.getRota(),
                coleta.getVeiculoId(),
                coleta.getQuilometragem(),
                coleta.getPesagemKg(),
                coleta.getAtivo()
        );
    }
}