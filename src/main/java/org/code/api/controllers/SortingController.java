package org.code.api.controllers;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.domain.enums.SortingType;
import org.code.api.domain.ports.SortingPort;
import org.code.api.dto.sorting.request.SortingCreateRequestDTO;
import org.code.api.dto.sorting.response.SortingResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * HTTP boundary for organization-owned sorting use cases; Swagger is the official contract.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/sortings", "/api/v1/sortings"})
public class SortingController {

  private final SortingPort sortingPort;

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<SortingResponseDTO> create(
      @Valid @RequestBody SortingCreateRequestDTO data) {
    SortingResponseDTO response = sortingPort.create(data);

    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Page<SortingResponseDTO>> list(
      @RequestParam(required = false) SortingType sortingType,
      @PageableDefault(size = 20, sort = "sortingDate") Pageable pageable) {
    return ResponseEntity.ok(sortingPort.list(sortingType, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<SortingResponseDTO> getById(@PathVariable UUID id) {
    return ResponseEntity.ok(sortingPort.getById(id));
  }
}
