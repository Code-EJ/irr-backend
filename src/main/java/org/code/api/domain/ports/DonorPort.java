package org.code.api.domain.ports;

import java.util.UUID;
import org.code.api.dto.donor.request.DonorCreateRequestDTO;
import org.code.api.dto.donor.request.DonorUpdateRequestDTO;
import org.code.api.dto.donor.response.DonorResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Donor Port boundary for the IRR application.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public interface DonorPort {

  DonorResponseDTO create(DonorCreateRequestDTO data);

  DonorResponseDTO getById(UUID id);

  DonorResponseDTO update(UUID id, DonorUpdateRequestDTO data);

  void deactivate(UUID id);

  Page<DonorResponseDTO> list(Pageable pageable);
}
