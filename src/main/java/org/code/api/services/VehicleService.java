package org.code.api.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.exception.VehicleError;
import org.code.api.domain.models.base.Vehicle;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.domain.ports.VehiclePort;
import org.code.api.dto.logistic.vehicle.request.*;
import org.code.api.dto.logistic.vehicle.response.VehicleResponseDTO;
import org.code.api.infrastructure.repositories.CollectionRepository;
import org.code.api.infrastructure.repositories.UserRepository;
import org.code.api.infrastructure.repositories.VehicleRepository;
import org.code.api.infrastructure.specifications.VehicleSpecification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável pela orquestração das regras de negócio relacionadas à entidade {@link Vehicle}.
 *
 * <p>Todas as operações de leitura e escrita filtram pelo {@code creator_id}
 * do usuário autenticado, garantindo isolamento de dados multilocatário.</p>
 */
@Slf4j
@Service
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class VehicleService implements VehiclePort {
    private final ReferenceLifecycleGuard lifecycle;

    private final VehicleRepository vehicleRepository;
    private final CollectionRepository collectionRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider userProvider;
    private final org.code.api.domain.ports.OrganizationScope scope;

    // ═══════════════════════════════════════════════════════════════════════════
    // Operações Unitárias
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @PreAuthorize("@organizationScope.manager()")
    @Transactional
    public VehicleResponseDTO create(VehicleCreateRequestDTO data) {
        UUID organizationId = scope.organizationId();
        if (!scope.manager()) throw new org.springframework.security.access.AccessDeniedException("Organization manager permission is required");
        User creator = userRepository.getReferenceById(userProvider.getCurrentUserId());

        String licensePlate = normalizePlate(data.licensePlate());
        String model = data.model() != null ? data.model().trim() : null;

        if (vehicleRepository.existsByLicensePlateAndOrganizationId(licensePlate, organizationId)) {
            throw new VehicleError.PlateAlreadyExists(licensePlate);
        }

        try {
            Vehicle vehicle = vehicleRepository.save(
                    Vehicle.builder()
                            .licensePlate(licensePlate)
                            .model(model)
                            .isActive(true)
                            .creator(creator).organizationId(organizationId)
                            .build()
            );

            return toResponse(vehicle);
        } catch (DataIntegrityViolationException e) {
            throw new VehicleError.PlateAlreadyExists(licensePlate);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponseDTO getById(UUID id) {
        UUID organizationId = scope.organizationId();

        Vehicle vehicle = vehicleRepository
                .findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new VehicleError.NotFound(id));

        return toResponse(vehicle);
    }

    @Override
    @PreAuthorize("@organizationScope.manager()")
    @Transactional
    public VehicleResponseDTO update(UUID id, VehicleUpdateRequestDTO data) {
        UUID organizationId = scope.organizationId();
        if (!scope.manager()) throw new org.springframework.security.access.AccessDeniedException("Organization manager permission is required");

        Vehicle vehicle = vehicleRepository
                .findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new VehicleError.NotFound(id));

        if (!vehicle.getIsActive()) {
            throw new VehicleError.InactiveVehicle(id);
        }

        String newPlate = normalizePlate(data.licensePlate());
        String newModel = data.model() != null ? data.model().trim() : null;

        if (!vehicle.getLicensePlate().equals(newPlate)) {
            vehicleRepository.findByLicensePlateAndOrganizationId(newPlate, organizationId).ifPresent(found -> {
                throw new VehicleError.PlateAlreadyExists(newPlate);
            });
        }

        vehicle.setLicensePlate(newPlate);
        vehicle.setModel(newModel);
        if (Boolean.FALSE.equals(data.isActive())) lifecycle.vehicle(id);
        vehicle.setIsActive(data.isActive());

        try {
            Vehicle updatedVehicle = vehicleRepository.save(vehicle);
            return toResponse(updatedVehicle);
        } catch (DataIntegrityViolationException e) {
            throw new VehicleError.PlateAlreadyExists(newPlate);
        }
    }

    @Override
    @PreAuthorize("@organizationScope.manager()")
    @Transactional
    public void deactivate(UUID id) {
        UUID organizationId = scope.organizationId();
        if (!scope.manager()) throw new org.springframework.security.access.AccessDeniedException("Organization manager permission is required");
        List<UserRole> roles = userProvider.getCurrentUserRoles();
        boolean isAdmin = roles.contains(UserRole.ADMINISTRATOR);

        Vehicle vehicle = vehicleRepository
                .findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new VehicleError.NotFound(id));
        lifecycle.vehicle(id);

        if (!vehicle.getIsActive()) {
            throw new VehicleError.InactiveVehicle(id);
        }

        // Critério 3: não-admin bloqueado se houver coletas vinculadas
        boolean hasCollections = collectionRepository.existsByVehicleId(id);
        if (hasCollections) {
            throw new VehicleError.HasCollectionBinding(id);
        }

        // Critério 4: admin pode desativar mesmo com vínculo (soft delete)
        vehicle.setIsActive(false);
        vehicleRepository.save(vehicle);

        log.info("Vehicle {} deactivated (admin={}, had_collections={})", id, isAdmin, hasCollections);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Listagem com Filtragem Dinâmica
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResponseDTO> list(String licensePlate, String model, Pageable pageable) {
        UUID organizationId = scope.organizationId();

        // Base: sempre filtra pelo criador (isolamento multilocatário)
        Specification<Vehicle> spec = VehicleSpecification.withOrganizationId(organizationId);

        // Filtros opcionais — composição dinâmica
        if (licensePlate != null && !licensePlate.isBlank()) {
            spec = spec.and(VehicleSpecification.licensePlateContains(licensePlate));
        }
        if (model != null && !model.isBlank()) {
            spec = spec.and(VehicleSpecification.modelContains(model));
        }

        return vehicleRepository.findAll(spec, pageable).map(this::toResponse);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Operações em Massa (Bulk)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @PreAuthorize("@organizationScope.manager()")
    @Transactional
    public List<VehicleResponseDTO> bulkCreate(VehicleBulkCreateRequestDTO data) {
        UUID organizationId = scope.organizationId();
        if (!scope.manager()) throw new org.springframework.security.access.AccessDeniedException("Organization manager permission is required");
        User creator = userRepository.getReferenceById(userProvider.getCurrentUserId());

        // Validação prévia: detectar placas duplicadas dentro do próprio lote
        Set<String> plateBatch = new HashSet<>();
        for (VehicleCreateRequestDTO item : data.vehicles()) {
            String normalized = normalizePlate(item.licensePlate());
            if (!plateBatch.add(normalized)) {
                throw new VehicleError.PlateAlreadyExists(normalized);
            }
        }

        // Validação prévia: detectar placas que já existem no banco
        for (String plate : plateBatch) {
            if (vehicleRepository.existsByLicensePlateAndOrganizationId(plate, organizationId)) {
                throw new VehicleError.PlateAlreadyExists(plate);
            }
        }

        // Construção e persistência atômica do lote
        List<Vehicle> vehicles = new ArrayList<>();
        for (VehicleCreateRequestDTO item : data.vehicles()) {
            vehicles.add(
                Vehicle.builder()
                    .licensePlate(normalizePlate(item.licensePlate()))
                    .model(item.model() != null ? item.model().trim() : null)
                    .isActive(true)
                    .creator(creator).organizationId(organizationId)
                    .build()
            );
        }

        try {
            List<Vehicle> saved = vehicleRepository.saveAll(vehicles);
            return saved.stream().map(this::toResponse).toList();
        } catch (DataIntegrityViolationException e) {
            log.error("Bulk create failed due to data integrity violation", e);
            throw new VehicleError.PlateAlreadyExists("(bulk operation — check for duplicate plates)");
        }
    }

    @Override
    @PreAuthorize("@organizationScope.manager()")
    @Transactional
    public List<VehicleResponseDTO> bulkUpdate(VehicleBulkUpdateRequestDTO data) {
        UUID organizationId = scope.organizationId();
        if (!scope.manager()) throw new org.springframework.security.access.AccessDeniedException("Organization manager permission is required");

        // Validação prévia: detectar IDs duplicados no lote
        Set<UUID> idBatch = new HashSet<>();
        for (VehicleBulkUpdateItemDTO item : data.vehicles()) {
            if (!idBatch.add(item.id())) {
                throw new VehicleError.NotFound(item.id());
            }
        }

        // Validação prévia: detectar placas duplicadas dentro do lote
        Set<String> plateBatch = new HashSet<>();
        for (VehicleBulkUpdateItemDTO item : data.vehicles()) {
            String normalized = normalizePlate(item.licensePlate());
            if (!plateBatch.add(normalized)) {
                throw new VehicleError.PlateAlreadyExists(normalized);
            }
        }

        List<VehicleResponseDTO> results = new ArrayList<>();

        for (VehicleBulkUpdateItemDTO item : data.vehicles()) {
            Vehicle vehicle = vehicleRepository
                    .findByIdAndOrganizationId(item.id(), organizationId)
                    .orElseThrow(() -> new VehicleError.NotFound(item.id()));

            if (!vehicle.getIsActive()) {
                throw new VehicleError.InactiveVehicle(item.id());
            }

            String newPlate = normalizePlate(item.licensePlate());
            String newModel = item.model() != null ? item.model().trim() : null;

            // Verificar conflito de placa somente se mudou
            if (!vehicle.getLicensePlate().equals(newPlate)) {
                vehicleRepository.findByLicensePlateAndOrganizationId(newPlate, organizationId).ifPresent(found -> {
                    if (!found.getId().equals(item.id())) {
                        throw new VehicleError.PlateAlreadyExists(newPlate);
                    }
                });
            }

            vehicle.setLicensePlate(newPlate);
            vehicle.setModel(newModel);
            if (Boolean.FALSE.equals(item.isActive())) lifecycle.vehicle(vehicle.getId());
            vehicle.setIsActive(item.isActive());

            results.add(toResponse(vehicleRepository.save(vehicle)));
        }

        return results;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Métodos privados
    // ═══════════════════════════════════════════════════════════════════════════

    private String normalizePlate(String plate) {
        return plate.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private VehicleResponseDTO toResponse(Vehicle vehicle) {
        return new VehicleResponseDTO(
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getModel(),
                vehicle.getIsActive(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt(),
                vehicle.getCreator() != null
                        ? vehicle.getCreator().getId().toString()
                        : null
        );
    }
}
