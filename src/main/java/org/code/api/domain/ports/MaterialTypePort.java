package org.code.api.domain.ports;

import org.code.api.dto.material.request.MaterialTypeCreateRequestDTO;
import org.code.api.dto.material.request.MaterialTypeUpdateRequestDTO;
import org.code.api.dto.material.response.MaterialTypeResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Porta de entrada para operações sobre Tipos de Material (2° nível da árvore).
 */
public interface MaterialTypePort {

    MaterialTypeResponseDTO create(MaterialTypeCreateRequestDTO data);

    Page<MaterialTypeResponseDTO> list(UUID categoryId, String name, Pageable pageable);

    MaterialTypeResponseDTO getById(UUID id);

    MaterialTypeResponseDTO update(UUID id, MaterialTypeUpdateRequestDTO data);

    void deactivate(UUID id);
}
