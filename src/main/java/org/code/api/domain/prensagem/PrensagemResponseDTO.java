package org.code.api.domain.prensagem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PrensagemResponseDTO(
    UUID id,
    LocalDateTime data,
    BigDecimal volumeTotal,
    TipoOrigemDestino tipoOrigem,
    TipoOrigemDestino tipoDestino,
    UUID subtipologiaId,
    String subtipologiaNome,
    String tipologiaNome,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public PrensagemResponseDTO(Prensagem prensagem) {
        this(
            prensagem.getId(),
            prensagem.getData(),
            prensagem.getVolumeTotal(),
            prensagem.getTipoOrigem(),
            prensagem.getTipoDestino(),
            prensagem.getSubtipologia() != null ? prensagem.getSubtipologia().getId() : null,
            prensagem.getSubtipologia() != null ? prensagem.getSubtipologia().getNome() : null,
            prensagem.getSubtipologia() != null && prensagem.getSubtipologia().getTipologia() != null
                ? prensagem.getSubtipologia().getTipologia().getNome() : null,
            prensagem.getCreatedAt(),
            prensagem.getUpdatedAt()
        );
    }
}
