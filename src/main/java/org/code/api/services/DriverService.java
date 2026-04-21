package org.code.api.services;

import java.util.List;
import java.util.stream.Collectors;

import org.code.api.domain.models.logistic.Driver;
import org.code.api.domain.ports.DriverPort;
import org.code.api.dto.driver.DriverRequestDTO;
import org.code.api.dto.driver.DriverResponseDTO;
import org.code.api.exceptions.DuplicateResourceException;
import org.code.api.exceptions.ResourceNotFoundException;
import org.code.api.repository.DriverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DriverService implements DriverPort {

    private final DriverRepository driverRepository;

    @Transactional
    public DriverResponseDTO create(DriverRequestDTO requestDTO) {
        // Verificar se CPF já existe
        if (driverRepository.existsByCpf(requestDTO.getCpf())) {
            throw new DuplicateResourceException("CPF já cadastrado: " + requestDTO.getCpf());
        }

            Driver driver = Driver.builder()
                    .nome(requestDTO.getNome())
                    .cpf(requestDTO.getCpf())
                    .build();

        Driver savedDriver = driverRepository.save(driver);
        return mapToResponseDTO(savedDriver);
    }

    @Transactional(readOnly = true)
    public List<DriverResponseDTO> findAll() {
        return driverRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DriverResponseDTO findById(Integer id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado com ID: " + id));
        return mapToResponseDTO(driver);
    }

    @Transactional(readOnly = true)
    public DriverResponseDTO findByCpf(String cpf) {
        Driver driver = driverRepository.findByCpf(cpf)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado com CPF: " + cpf));
        return mapToResponseDTO(driver);
    }

    @Transactional
    public DriverResponseDTO update(Integer id, DriverRequestDTO requestDTO) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado com ID: " + id));

        // Verificar se o novo CPF já existe em outro registro
        if (!driver.getCpf().equals(requestDTO.getCpf()) && 
            driverRepository.existsByCpf(requestDTO.getCpf())) {
            throw new DuplicateResourceException("CPF já cadastrado: " + requestDTO.getCpf());
        }

        driver.setNome(requestDTO.getNome());
        driver.setCpf(requestDTO.getCpf());

        Driver updatedDriver = driverRepository.save(driver);
        return mapToResponseDTO(updatedDriver);
    }

    @Transactional
    public void delete(Integer id) {
        if (!driverRepository.existsById(id)) {
            throw new ResourceNotFoundException("Motorista não encontrado com ID: " + id);
        }
        driverRepository.deleteById(id);
    }

    private DriverResponseDTO mapToResponseDTO(Driver driver) {
        return DriverResponseDTO.builder()
                .id(driver.getId())
                .nome(driver.getNome())
                .cpf(driver.getCpf())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }
}
