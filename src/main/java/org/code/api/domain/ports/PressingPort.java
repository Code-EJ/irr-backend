package org.code.api.domain.ports;

import java.util.UUID;
import org.code.api.dto.pressing.request.PressingCreateRequestDTO;
import org.code.api.dto.pressing.response.PressingResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Inbound use cases for organization-owned pressing records.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public interface PressingPort {

  PressingResponseDTO create(PressingCreateRequestDTO data);

  Page<PressingResponseDTO> list(Pageable pageable);

  PressingResponseDTO getById(UUID id);
}
