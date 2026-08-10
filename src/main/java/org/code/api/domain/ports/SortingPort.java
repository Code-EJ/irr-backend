package org.code.api.domain.ports;

import org.code.api.domain.enums.SortingType;
import org.code.api.dto.sorting.request.SortingCreateRequestDTO;
import org.code.api.dto.sorting.response.SortingResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Porta de entrada para operações de Triagem de Materiais.
 */
public interface SortingPort {

    SortingResponseDTO create(SortingCreateRequestDTO data);

    Page<SortingResponseDTO> list(SortingType sortingType, Pageable pageable);

    SortingResponseDTO getById(UUID id);
}
