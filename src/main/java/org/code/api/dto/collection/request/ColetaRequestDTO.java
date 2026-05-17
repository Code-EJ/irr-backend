package org.code.api.dto.collection.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ColetaRequestDTO(
        LocalDateTime horarioSaida,
        LocalDateTime horarioChegada,
        String rota,
        Long veiculoId,
        BigDecimal quilometragem,
        BigDecimal pesagemKg
) {
}