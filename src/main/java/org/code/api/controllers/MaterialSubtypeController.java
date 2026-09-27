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
 * Exposes material subtype operations in the authenticated creator scope.
 * Reads require authentication; writes require ADMINISTRATOR.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/materials/subtypes")
public class MaterialSubtypeController {

    private final MaterialSubtypePort subtypePort;

    /** Creates a material for the current administrator. */
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

    /** Lists matching active materials; name and parent filters precede pagination. */
    @io.swagger.v3.oas.annotations.Operation(description = "Lists only the authenticated creator's active materials. The optional name filter is a trimmed, case-insensitive literal substring; percent and underscore are not wildcards. Filtering precedes pagination and total counts.")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MaterialSubtypeResponseDTO>> list(
        @RequestParam(required = false) UUID typeId,
        @RequestParam(required = false) String name,
        @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(subtypePort.list(typeId, name, pageable));
    }

    /** Reads an owned material; missing and foreign IDs return 404. */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MaterialSubtypeResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(subtypePort.getById(id));
    }

    /** Updates an owned material using the expected version. */
    @io.swagger.v3.oas.annotations.Operation(description = "Administrator-only update in the authenticated creator scope. Send the last returned version. Stale versions return 409; refresh the record before retrying. Successful responses include the flushed server-managed version.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<MaterialSubtypeResponseDTO> update(
        @PathVariable UUID id,
        @Valid @RequestBody MaterialSubtypeUpdateRequestDTO data
    ) {
        return ResponseEntity.ok(subtypePort.update(id, data));
    }

    /** Applies the existing owned-material deactivation policy. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        subtypePort.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
