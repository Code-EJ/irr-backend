package org.code.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
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
 * Exposes material category operations in the authenticated creator scope.
 * Reads require authentication; writes require ADMINISTRATOR.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/materials/categories", "/api/v1/materials/categories"})
public class MaterialCategoryController {

    private final MaterialCategoryPort categoryPort;

    /** Creates a material for the current administrator. */
    @PostMapping
    @PreAuthorize("@organizationScope.manager()")
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

    /** Lists matching active materials; name and parent filters precede pagination. */
    @Operation(description = "Lists only the authenticated creator's active materials. The optional name filter is a trimmed, case-insensitive literal substring; percent and underscore are not wildcards. Filtering precedes pagination and total counts.")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MaterialCategoryResponseDTO>> list(
        @RequestParam(required = false) String name,
        @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(categoryPort.list(name, pageable));
    }

    /** Reads an owned material; missing and foreign IDs return 404. */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MaterialCategoryResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryPort.getById(id));
    }

    /** Updates an owned material using the expected version. */
    @Operation(description = "Administrator-only update in the authenticated creator scope. Send the last returned version. Stale versions return 409; refresh the record before retrying. Successful responses include the flushed server-managed version.")
    @PutMapping("/{id}")
    @PreAuthorize("@organizationScope.manager()")
    public ResponseEntity<MaterialCategoryResponseDTO> update(
        @PathVariable UUID id,
        @Valid @RequestBody MaterialCategoryUpdateRequestDTO data
    ) {
        return ResponseEntity.ok(categoryPort.update(id, data));
    }

    /** Applies the existing owned-material deactivation policy. */
    @DeleteMapping("/{id}")
    @PreAuthorize("@organizationScope.manager()")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        categoryPort.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
