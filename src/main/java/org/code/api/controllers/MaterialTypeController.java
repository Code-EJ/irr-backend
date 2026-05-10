package org.code.api.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.code.api.domain.ports.MaterialTypePort;
import org.code.api.dto.material.request.MaterialTypeCreateRequestDTO;
import org.code.api.dto.material.request.MaterialTypeUpdateRequestDTO;
import org.code.api.dto.material.response.MaterialTypeResponseDTO;
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
 * Controller REST para Tipos de Material (2° nível da árvore tipológica).
 *
 * <p>Leitura: qualquer autenticado. Escrita/Exclusão: apenas ADMINISTRATOR.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/materials/types")
public class MaterialTypeController {

    private final MaterialTypePort typePort;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<MaterialTypeResponseDTO> create(
        @Valid @RequestBody MaterialTypeCreateRequestDTO data
    ) {
        MaterialTypeResponseDTO response = typePort.create(data);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MaterialTypeResponseDTO>> list(
        @RequestParam(required = false) UUID categoryId,
        @RequestParam(required = false) String name,
        @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(typePort.list(categoryId, name, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MaterialTypeResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(typePort.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<MaterialTypeResponseDTO> update(
        @PathVariable UUID id,
        @Valid @RequestBody MaterialTypeUpdateRequestDTO data
    ) {
        return ResponseEntity.ok(typePort.update(id, data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        typePort.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
