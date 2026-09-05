package org.code.api.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.code.api.domain.ports.PressingPort;
import org.code.api.dto.pressing.request.PressingCreateRequestDTO;
import org.code.api.dto.pressing.response.PressingResponseDTO;
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
 * Controller REST para gestão do processo de Prensagem de Materiais.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pressings")
public class PressingController {

    private final PressingPort pressingPort;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL')")
    public ResponseEntity<PressingResponseDTO> create(
        @Valid @RequestBody PressingCreateRequestDTO data
    ) {
        PressingResponseDTO response = pressingPort.create(data);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PressingResponseDTO>> list(
        @PageableDefault(size = 20, sort = "pressingDate") Pageable pageable
    ) {
        return ResponseEntity.ok(pressingPort.list(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PressingResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(pressingPort.getById(id));
    }
}
