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
 * Exposes material type operations in the authenticated creator scope.
 * Reads require authentication; writes require ADMINISTRATOR.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/materials/types")
public class MaterialTypeController {

    private final MaterialTypePort typePort;

    /** Creates a material for the current administrator. */
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

    /** Lists matching active materials; name and parent filters precede pagination. */
    @io.swagger.v3.oas.annotations.Operation(description = "Lists only the authenticated creator's active materials. The optional name filter is a trimmed, case-insensitive literal substring; percent and underscore are not wildcards. Filtering precedes pagination and total counts.")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MaterialTypeResponseDTO>> list(
        @RequestParam(required = false) UUID categoryId,
        @RequestParam(required = false) String name,
        @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(typePort.list(categoryId, name, pageable));
    }

    /** Reads an owned material; missing and foreign IDs return 404. */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MaterialTypeResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(typePort.getById(id));
    }

    /** Updates an owned material using the expected version. */
    @io.swagger.v3.oas.annotations.Operation(description = "Administrator-only update in the authenticated creator scope. Send the last returned version. Stale versions return 409; refresh the record before retrying. Successful responses include the flushed server-managed version.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<MaterialTypeResponseDTO> update(
        @PathVariable UUID id,
        @Valid @RequestBody MaterialTypeUpdateRequestDTO data
    ) {
        return ResponseEntity.ok(typePort.update(id, data));
    }

    /** Applies the existing owned-material deactivation policy. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        typePort.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
