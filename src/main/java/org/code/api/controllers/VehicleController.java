package org.code.api.controllers;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.domain.ports.VehiclePort;
import org.code.api.dto.logistic.vehicle.request.VehicleBulkCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleBulkUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.response.VehicleResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Organization-owned fleet endpoints with manager-controlled individual and atomic batch writes.
 * Lists apply organization and optional literal filters before pagination. Referenced vehicles
 * remain retained; administrator status does not bypass historical reference protection.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

  private final VehiclePort vehiclePort;

  @PostMapping
  @PreAuthorize("@organizationScope.manager()")
  public ResponseEntity<VehicleResponseDTO> create(
      @Valid @RequestBody VehicleCreateRequestDTO data) {
    VehicleResponseDTO response = vehiclePort.create(data);

    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

    return ResponseEntity.created(location).body(response);
  }

  @PostMapping("/batch")
  @PreAuthorize("@organizationScope.manager()")
  public ResponseEntity<List<VehicleResponseDTO>> bulkCreate(
      @Valid @RequestBody VehicleBulkCreateRequestDTO data) {
    List<VehicleResponseDTO> created = vehiclePort.bulkCreate(data);
    return ResponseEntity.status(201).body(created);
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Page<VehicleResponseDTO>> list(
      @RequestParam(required = false) String licensePlate,
      @RequestParam(required = false) String model,
      @PageableDefault(size = 20, sort = "licensePlate") Pageable pageable) {
    return ResponseEntity.ok(vehiclePort.list(licensePlate, model, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<VehicleResponseDTO> getById(@PathVariable UUID id) {
    return ResponseEntity.ok(vehiclePort.getById(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@organizationScope.manager()")
  public ResponseEntity<VehicleResponseDTO> update(
      @PathVariable UUID id, @Valid @RequestBody VehicleUpdateRequestDTO data) {
    return ResponseEntity.ok(vehiclePort.update(id, data));
  }

  @PutMapping("/batch")
  @PreAuthorize("@organizationScope.manager()")
  public ResponseEntity<List<VehicleResponseDTO>> bulkUpdate(
      @Valid @RequestBody VehicleBulkUpdateRequestDTO data) {
    return ResponseEntity.ok(vehiclePort.bulkUpdate(data));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@organizationScope.manager()")
  public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
    vehiclePort.deactivate(id);
    return ResponseEntity.noContent().build();
  }
}
