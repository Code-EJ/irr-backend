package org.code.api.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.code.api.domain.ports.VehiclePort;
import org.code.api.dto.logistic.vehicle.request.VehicleBulkCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleBulkUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.response.VehicleResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gestão de Veículos da frota.
 *
 * <p>Expõe operações CRUD unitárias e em massa, com filtragem dinâmica paginada.
 * Proteção RBAC via {@code @PreAuthorize} em cada endpoint.</p>
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>{@code POST   /api/vehicles}          — Criação unitária</li>
 *   <li>{@code POST   /api/vehicles/batch}    — Criação em massa (até 100)</li>
 *   <li>{@code GET    /api/vehicles}           — Listagem paginada com filtros</li>
 *   <li>{@code GET    /api/vehicles/{id}}      — Busca por ID</li>
 *   <li>{@code PUT    /api/vehicles/{id}}      — Atualização unitária</li>
 *   <li>{@code PUT    /api/vehicles/batch}     — Atualização em massa (até 100)</li>
 *   <li>{@code DELETE /api/vehicles/{id}}      — Desativação (soft delete)</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehiclePort vehiclePort;

    // ── Criação ──────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL')")
    public ResponseEntity<VehicleResponseDTO> create(
        @Valid @RequestBody VehicleCreateRequestDTO data
    ) {
        VehicleResponseDTO response = vehiclePort.create(data);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL')")
    public ResponseEntity<List<VehicleResponseDTO>> bulkCreate(
        @Valid @RequestBody VehicleBulkCreateRequestDTO data
    ) {
        List<VehicleResponseDTO> created = vehiclePort.bulkCreate(data);
        return ResponseEntity.status(201).body(created);
    }

    // ── Leitura ──────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<VehicleResponseDTO>> list(
            @RequestParam(required = false) String licensePlate,
            @RequestParam(required = false) String model,
            @PageableDefault(size = 20, sort = "licensePlate") Pageable pageable
    ) {
        return ResponseEntity.ok(vehiclePort.list(licensePlate, model, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VehicleResponseDTO> getById(
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(vehiclePort.getById(id));
    }

    // ── Atualização ──────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL')")
    public ResponseEntity<VehicleResponseDTO> update(
        @PathVariable UUID id,
        @Valid @RequestBody VehicleUpdateRequestDTO data
    ) {
        return ResponseEntity.ok(vehiclePort.update(id, data));
    }

    @PutMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL')")
    public ResponseEntity<List<VehicleResponseDTO>> bulkUpdate(
        @Valid @RequestBody VehicleBulkUpdateRequestDTO data
    ) {
        return ResponseEntity.ok(vehiclePort.bulkUpdate(data));
    }

    // ── Exclusão (Soft Delete) ───────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL')")
    public ResponseEntity<Void> deactivate(
        @PathVariable UUID id
    ) {
        vehiclePort.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
