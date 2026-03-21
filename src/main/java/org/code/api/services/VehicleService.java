package org.code.api.services;

import java.util.List;
import lombok.AllArgsConstructor;
import org.code.api.domain.enums.UserType;
import org.code.api.domain.exception.VehicleError;
import org.code.api.domain.models.logistic.Vehicle;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.VehiclePort;
import org.code.api.dto.logistic.vehicle.request.VehicleCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.response.VehicleResponseDTO;
import org.code.api.infrastructure.repositories.UserRepository;
import org.code.api.infrastructure.repositories.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class VehicleService implements VehiclePort {

    private VehicleRepository vehicleRepository;
    private UserRepository userRepository;

    @Override
    @Transactional
    public VehicleResponseDTO create(VehicleCreateRequestDTO data, Session session) {
        String placa = normalizePlate(data.placa());
        String modelo = normalizeModel(data.modelo());

        if (vehicleRepository.existsByPlaca(placa)) {
            throw new VehicleError.PlateAlreadyExists(placa);
        }

        User createdBy = userRepository
            .findById(session.getId())
            .orElseThrow(() ->
                new VehicleError.SessionUserNotFound(session.getId().toString())
            );

        Vehicle vehicle = vehicleRepository.save(
            Vehicle.builder()
                .placa(placa)
                .modelo(modelo)
                .ativo(true)
                .createdBy(createdBy)
                .build()
        );

        return toResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponseDTO> list(Session session) {
        return vehicleRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponseDTO getById(Integer id, Session session) {
        Vehicle vehicle = vehicleRepository
            .findById(id)
            .orElseThrow(() -> new VehicleError.NotFound(id));

        return toResponse(vehicle);
    }

    @Override
    @Transactional
    public VehicleResponseDTO update(
        Integer id,
        VehicleUpdateRequestDTO data,
        Session session
    ) {
        Vehicle vehicle = vehicleRepository
            .findById(id)
            .orElseThrow(() -> new VehicleError.NotFound(id));

        String placa = normalizePlate(data.placa());
        String modelo = normalizeModel(data.modelo());

        vehicleRepository
            .findByPlaca(placa)
            .ifPresent(found -> {
                if (!found.getId().equals(id)) {
                    throw new VehicleError.PlateAlreadyExists(placa);
                }
            });

        vehicle.setPlaca(placa);
        vehicle.setModelo(modelo);
        vehicle.setAtivo(data.ativo());

        Vehicle updated = vehicleRepository.save(vehicle);

        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deactivate(Integer id, Session session) {
        if (session.getTipo() != UserType.ADMINISTRADOR) {
            throw new VehicleError.AccessDenied(session.getTipo());
        }

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
