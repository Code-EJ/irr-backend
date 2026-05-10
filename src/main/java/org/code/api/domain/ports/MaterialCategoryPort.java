package org.code.api.domain.ports;

import org.code.api.dto.material.request.MaterialCategoryCreateRequestDTO;
import org.code.api.dto.material.request.MaterialCategoryUpdateRequestDTO;
import org.code.api.dto.material.response.MaterialCategoryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Porta de entrada para operações sobre Categorias de Material (1° nível da árvore).
 */
public interface MaterialCategoryPort {

    MaterialCategoryResponseDTO create(MaterialCategoryCreateRequestDTO data);

    Page<MaterialCategoryResponseDTO> list(String name, Pageable pageable);

    MaterialCategoryResponseDTO getById(UUID id);

    MaterialCategoryResponseDTO update(UUID id, MaterialCategoryUpdateRequestDTO data);

    void deactivate(UUID id);
}
