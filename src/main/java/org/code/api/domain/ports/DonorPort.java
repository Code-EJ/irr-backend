package org.code.api.domain.ports;

import org.code.api.dto.donor.request.DonorCreateRequestDTO;
import org.code.api.dto.donor.request.DonorUpdateRequestDTO;
import org.code.api.dto.donor.response.DonorResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DonorPort {

    DonorResponseDTO create(DonorCreateRequestDTO data);

    DonorResponseDTO getById(UUID id);

    DonorResponseDTO update(UUID id, DonorUpdateRequestDTO data);

    void deactivate(UUID id);

    Page<DonorResponseDTO> list(Pageable pageable);
}