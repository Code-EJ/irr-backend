package org.code.api.domain.ports;

import java.util.List;
import java.util.UUID;
import org.code.api.dto.logistic.vehicle.request.VehicleBulkCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleBulkUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.response.VehicleResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Inbound fleet use cases with explicit organization ownership and manager-controlled writes.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public interface VehiclePort {

  // ── Individual operations ──────────────────────────────────────────────────

  VehicleResponseDTO create(VehicleCreateRequestDTO data);

  VehicleResponseDTO getById(UUID id);

  VehicleResponseDTO update(UUID id, VehicleUpdateRequestDTO data);

  void deactivate(UUID id);

  // ── Scoped listing with optional filters ──────────────────────────────────────

  /**
   * Lists active organization vehicles with optional literal plate/model filters applied before
   * pagination.
   */
  Page<VehicleResponseDTO> list(String licensePlate, String model, Pageable pageable);

  // ── Atomic batch operations ────────────────────────────────────────────

  /** Creates the full vehicle batch atomically; any invalid item rolls back all changes. */
  List<VehicleResponseDTO> bulkCreate(VehicleBulkCreateRequestDTO data);

  /** Replaces the full vehicle batch atomically; any invalid item rolls back all changes. */
  List<VehicleResponseDTO> bulkUpdate(VehicleBulkUpdateRequestDTO data);
}
