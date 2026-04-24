package org.code.api.dto.document;

import org.code.api.domain.models.document.Document;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponseDTO(UUID id, String nome, LocalDateTime datadDocumento, LocalDateTime dataInsercao) {
    public DocumentResponseDTO(Document document) {
        this(document.getId(), document.getNome(), document.getDataDocumento(), document.getCreatedAt());
    }

}
