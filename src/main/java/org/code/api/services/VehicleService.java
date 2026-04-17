package org.code.api.services;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.code.api.domain.exception.VehicleError;
import org.code.api.domain.models.logistic.Vehicle;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.domain.ports.VehiclePort;
import org.code.api.dto.logistic.vehicle.request.VehicleCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.response.VehicleResponseDTO;
import org.code.api.infrastructure.repositories.UserRepository;
import org.code.api.infrastructure.repositories.VehicleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VehicleService implements VehiclePort {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider userProvider;

    /// @Note: O banco de dados tem uma UNIQUE CONSTRAINT no campo "placa", tenha em mente que sem isso o método abaixo irá causar uma race condition.
    @Override
    @Transactional
    public VehicleResponseDTO create(VehicleCreateRequestDTO data) {

        String placa = normalizePlate(data.placa());
        String modelo = normalizeModel(data.modelo());

        if (vehicleRepository.existsByPlaca(placa)) {
            throw new VehicleError.PlateAlreadyExists(placa);
        }

        try {
            UUID userId = (UUID) userProvider.getCurrentUserId();

            User createdBy = userRepository.getReferenceById(userId);

            Vehicle vehicle = vehicleRepository.save(
                    Vehicle.builder()
                            .placa(placa)
                            .modelo(modelo)
                            .ativo(true)
                            .createdBy(createdBy)
                            .build()
            );

            return toResponse(vehicle);
        } catch (DataIntegrityViolationException e) {
            throw new VehicleError.PlateAlreadyExists(data.placa());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResponseDTO> list(Pageable pageable) {
        return vehicleRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponseDTO getById(Integer id) {
        Vehicle vehicle = vehicleRepository
                .findById(id)
                .orElseThrow(() -> new VehicleError.NotFound(id));
        return toResponse(vehicle);
    }

    /// @Note: Mesma situação de Race condition que o método {@code create}
    @Override
    @Transactional
    public VehicleResponseDTO update(
            Integer id,
            VehicleUpdateRequestDTO data
    ) {

        Vehicle vehicle = vehicleRepository
                .findById(id)
                .orElseThrow(() -> new VehicleError.NotFound(id));

        String novaPlaca = normalizePlate(data.placa());
        String novoModelo = normalizeModel(data.modelo());

        // Só verificamos se uma placa já existe no banco de dados, se o campo placa estiver sendo alterado. Isso otimiza o tempo de query.
        if(!vehicle.getPlaca().equals(novaPlaca)) {
            vehicleRepository.findByPlaca(novaPlaca).ifPresent(found-> {
                throw new VehicleError.PlateAlreadyExists(novaPlaca);
            });
        }

        vehicle.setPlaca(novaPlaca);
        vehicle.setModelo(novoModelo);
        vehicle.setAtivo(data.ativo());

        try {
            Vehicle updatedVehicle = vehicleRepository.save(vehicle);
            return toResponse(updatedVehicle);
        } catch (DataIntegrityViolationException e) {
            throw new VehicleError.PlateAlreadyExists(data.placa());
        }

    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void deactivate(Integer id) {

        Vehicle vehicle = vehicleRepository
                .findById(id)
                .orElseThrow(() -> new VehicleError.NotFound(id));

        vehicle.setAtivo(false);
        vehicleRepository.save(vehicle);
    }

    private String normalizePlate(String placa) {
        return placa.trim().toUpperCase();
    }

    private String normalizeModel(String modelo) {
        return modelo.trim();
    }

    private VehicleResponseDTO toResponse(Vehicle vehicle) {
        return new VehicleResponseDTO(
                vehicle.getId(),
                vehicle.getPlaca(),
                vehicle.getModelo(),
                vehicle.getAtivo(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt(),
                vehicle.getCreatedBy() != null
                        ? vehicle.getCreatedBy().getId().toString()
                        : null
        );
    }
}
