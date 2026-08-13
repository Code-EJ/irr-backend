package org.code.api.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.code.api.domain.ports.DonorPort;
import org.code.api.dto.donor.request.DonorCreateRequestDTO;
import org.code.api.dto.donor.request.DonorUpdateRequestDTO;
import org.code.api.dto.donor.response.DonorResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Controller REST para gestão de Doadores (Pessoa Física ou Jurídica).
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>{@code POST   /api/donors}       — Criação</li>
 *   <li>{@code GET    /api/donors}       — Listagem paginada</li>
 *   <li>{@code GET    /api/donors/{id}}  — Busca por ID</li>
 *   <li>{@code PUT    /api/donors/{id}}  — Atualização</li>
 *   <li>{@code DELETE /api/donors/{id}}  — Desativação (soft delete)</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/donors")
public class DonorController {

    private final DonorPort donorPort;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL')")
    public ResponseEntity<DonorResponseDTO> create(
            @Valid @RequestBody DonorCreateRequestDTO data
    ) {
        DonorResponseDTO response = donorPort.create(data);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DonorResponseDTO>> list(
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(donorPort.list(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonorResponseDTO> getById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(donorPort.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL')")
    public ResponseEntity<DonorResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody DonorUpdateRequestDTO data
    ) {
        return ResponseEntity.ok(donorPort.update(id, data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL')")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID id
    ) {
        donorPort.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}