package org.code.api.domain.ports;

import org.code.api.dto.donation.request.DonationCreateRequestDTO;
import org.code.api.dto.donation.request.DonationUpdateRequestDTO;
import org.code.api.dto.donation.response.DonationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DonationPort {

    DonationResponseDTO create(DonationCreateRequestDTO data);

    DonationResponseDTO getById(UUID id);

    DonationResponseDTO update(UUID id, DonationUpdateRequestDTO data);

    void deactivate(UUID id);

    Page<DonationResponseDTO> list(Pageable pageable);
}