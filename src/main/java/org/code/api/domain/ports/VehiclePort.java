package org.code.api.domain.ports;

import org.code.api.dto.logistic.vehicle.request.VehicleCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.response.VehicleResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VehiclePort {
    VehicleResponseDTO create(VehicleCreateRequestDTO data);

    Page<VehicleResponseDTO> list(Pageable pageable);

    VehicleResponseDTO getById(Integer id);

    VehicleResponseDTO update(
        Integer id,
        VehicleUpdateRequestDTO data
    );

    void deactivate(Integer id);
}
