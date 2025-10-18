package org.code.api.dto.document;

import java.util.Date;
import java.util.UUID;

public record DocumentRequestDTO(UUID id, String nome, Date dataDocumento, Date dataInsercao, Integer mediaId) {
}
