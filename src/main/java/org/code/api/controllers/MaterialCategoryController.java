package org.code.api.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.code.api.domain.ports.MaterialCategoryPort;
import org.code.api.dto.material.request.MaterialCategoryCreateRequestDTO;
import org.code.api.dto.material.request.MaterialCategoryUpdateRequestDTO;
import org.code.api.dto.material.response.MaterialCategoryResponseDTO;
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
 * Controller REST para Categorias de Material (1° nível da árvore tipológica).
 *
 * <p>Leitura: qualquer autenticado. Escrita/Exclusão: apenas ADMINISTRATOR.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/materials/categories")
public class MaterialCategoryController {

    private final MaterialCategoryPort categoryPort;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<MaterialCategoryResponseDTO> create(
        @Valid @RequestBody MaterialCategoryCreateRequestDTO data
    ) {
        MaterialCategoryResponseDTO response = categoryPort.create(data);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MaterialCategoryResponseDTO>> list(
        @RequestParam(required = false) String name,
        @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(categoryPort.list(name, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MaterialCategoryResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryPort.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<MaterialCategoryResponseDTO> update(
        @PathVariable UUID id,
        @Valid @RequestBody MaterialCategoryUpdateRequestDTO data
    ) {
        return ResponseEntity.ok(categoryPort.update(id, data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        categoryPort.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
