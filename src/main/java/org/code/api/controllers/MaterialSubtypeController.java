package org.code.api.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.code.api.domain.ports.MaterialSubtypePort;
import org.code.api.dto.material.request.MaterialSubtypeCreateRequestDTO;
import org.code.api.dto.material.request.MaterialSubtypeUpdateRequestDTO;
import org.code.api.dto.material.response.MaterialSubtypeResponseDTO;
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
 * Controller REST para Subtipos de Material (3° nível da árvore tipológica).
 *
 * <p>Leitura: qualquer autenticado. Escrita/Exclusão: apenas ADMINISTRATOR.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/materials/subtypes")
public class MaterialSubtypeController {

    private final MaterialSubtypePort subtypePort;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<MaterialSubtypeResponseDTO> create(
        @Valid @RequestBody MaterialSubtypeCreateRequestDTO data
    ) {
        MaterialSubtypeResponseDTO response = subtypePort.create(data);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MaterialSubtypeResponseDTO>> list(
        @RequestParam(required = false) UUID typeId,
        @RequestParam(required = false) String name,
        @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(subtypePort.list(typeId, name, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MaterialSubtypeResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(subtypePort.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<MaterialSubtypeResponseDTO> update(
        @PathVariable UUID id,
        @Valid @RequestBody MaterialSubtypeUpdateRequestDTO data
    ) {
        return ResponseEntity.ok(subtypePort.update(id, data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        subtypePort.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
