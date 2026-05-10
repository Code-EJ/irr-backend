package org.code.api.domain.ports;

import org.code.api.dto.material.request.MaterialSubtypeCreateRequestDTO;
import org.code.api.dto.material.request.MaterialSubtypeUpdateRequestDTO;
import org.code.api.dto.material.response.MaterialSubtypeResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Porta de entrada para operações sobre Subtipos de Material (3° nível da árvore).
 */
public interface MaterialSubtypePort {

    MaterialSubtypeResponseDTO create(MaterialSubtypeCreateRequestDTO data);

    Page<MaterialSubtypeResponseDTO> list(UUID typeId, String name, Pageable pageable);

    MaterialSubtypeResponseDTO getById(UUID id);

    MaterialSubtypeResponseDTO update(UUID id, MaterialSubtypeUpdateRequestDTO data);

    void deactivate(UUID id);
}
