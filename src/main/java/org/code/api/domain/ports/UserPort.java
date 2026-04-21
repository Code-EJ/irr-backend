package org.code.api.domain.ports;

import java.util.List;
import java.util.UUID;

import org.code.api.dto.user.UserRequestDTO;
import org.code.api.dto.user.UserResponseDTO;

public interface UserPort {
    
    UserResponseDTO create(UserRequestDTO request);

    List<UserResponseDTO> findAll();

    UserResponseDTO findById(UUID id);

    UserResponseDTO findByEmail(String email);

    UserResponseDTO update(UUID id, UserRequestDTO requestDTO);

    void delete(UUID id);

}
