package org.code.api.domain.ports;

import java.util.UUID;
import org.code.api.domain.enums.SortingType;
import org.code.api.dto.sorting.request.SortingCreateRequestDTO;
import org.code.api.dto.sorting.response.SortingResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Inbound use cases for organization-owned sorting records.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public interface SortingPort {

  SortingResponseDTO create(SortingCreateRequestDTO data);

  Page<SortingResponseDTO> list(SortingType sortingType, Pageable pageable);

  SortingResponseDTO getById(UUID id);
}
