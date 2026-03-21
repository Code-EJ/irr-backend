package org.code.api.domain.ports;

import java.util.List;
import org.code.api.domain.models.user.Session;
import org.code.api.dto.logistic.vehicle.request.VehicleCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.response.VehicleResponseDTO;

public interface VehiclePort {
    VehicleResponseDTO create(VehicleCreateRequestDTO data, Session session);

    List<VehicleResponseDTO> list(Session session);

    VehicleResponseDTO getById(Integer id, Session session);

    VehicleResponseDTO update(
        Integer id,
        VehicleUpdateRequestDTO data,
        Session session
    );

    void deactivate(Integer id, Session session);
}
