package org.code.api.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.code.api.domain.ports.DonationPort;
import org.code.api.dto.donation.request.DonationCreateRequestDTO;
import org.code.api.dto.donation.request.DonationUpdateRequestDTO;
import org.code.api.dto.donation.response.DonationResponseDTO;
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
 * Controller REST para gestão de Doações.
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>{@code POST   /api/donations}       — Criação</li>
 *   <li>{@code GET    /api/donations}       — Listagem paginada</li>
 *   <li>{@code GET    /api/donations/{id}}  — Busca por ID</li>
 *   <li>{@code PUT    /api/donations/{id}}  — Atualização</li>
 *   <li>{@code DELETE /api/donations/{id}}  — Desativação (soft delete)</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/donations")
public class DonationController {

    private final DonationPort donationPort;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL')")
    public ResponseEntity<DonationResponseDTO> create(
            @Valid @RequestBody DonationCreateRequestDTO data
    ) {
        DonationResponseDTO response = donationPort.create(data);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DonationResponseDTO>> list(
            @PageableDefault(size = 20, sort = "donationDate") Pageable pageable
    ) {
        return ResponseEntity.ok(donationPort.list(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonationResponseDTO> getById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(donationPort.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL')")
    public ResponseEntity<DonationResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody DonationUpdateRequestDTO data
    ) {
        return ResponseEntity.ok(donationPort.update(id, data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL')")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID id
    ) {
        donationPort.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}