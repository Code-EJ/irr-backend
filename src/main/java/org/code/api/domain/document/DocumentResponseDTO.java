package org.code.api.domain.document;

import java.util.Date;
import java.util.UUID;

public record DocumentResponseDTO(UUID id, String nome, Date datadDocumento, Date dataInsercao) {
    public DocumentResponseDTO(Document document) {
        this(document.getId(), document.getNome(), document.getDataDocumento(), document.getDataInsercao());
    }

}
