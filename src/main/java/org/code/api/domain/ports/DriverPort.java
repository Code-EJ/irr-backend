package org.code.api.domain.ports;

import java.util.List;
import org.code.api.dto.driver.DriverRequestDTO;
import org.code.api.dto.driver.DriverResponseDTO;

public interface DriverPort {
    DriverResponseDTO create(DriverRequestDTO requestDTO);

    List<DriverResponseDTO> findAll();

    DriverResponseDTO findById(Integer id);

    DriverResponseDTO update(Integer id, DriverRequestDTO requestDTO);

    void delete(Integer id);

}
