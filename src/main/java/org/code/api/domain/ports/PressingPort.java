package org.code.api.domain.ports;

import org.code.api.dto.pressing.request.PressingCreateRequestDTO;
import org.code.api.dto.pressing.response.PressingResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Porta de entrada para operações de Prensagem de Materiais.
 */
public interface PressingPort {

    PressingResponseDTO create(PressingCreateRequestDTO data);

    Page<PressingResponseDTO> list(Pageable pageable);

    PressingResponseDTO getById(UUID id);
}
