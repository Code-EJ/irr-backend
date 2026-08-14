package org.code.api.domain.prensagem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PrensagemRequestDTO(
    LocalDateTime data,
    UUID subtipologiaId,
    BigDecimal volumeTotal,
    TipoOrigemDestino tipoOrigem,
    TipoOrigemDestino tipoDestino
) {
}
