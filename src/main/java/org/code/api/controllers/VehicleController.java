package org.code.api.controllers;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.code.api.domain.models.user.Session;
import org.code.api.dto.logistic.vehicle.request.VehicleCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.response.VehicleResponseDTO;
import org.code.api.services.VehicleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/veiculo")
public class VehicleController {

    private VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<VehicleResponseDTO> create(
        @Valid @RequestBody VehicleCreateRequestDTO data,
        @RequestAttribute("session") Session session
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(vehicleService.create(data, session));
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponseDTO>> list(
        @RequestAttribute("session") Session session
    ) {
        return ResponseEntity.ok(vehicleService.list(session));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> getById(
        @PathVariable Integer id,
        @RequestAttribute("session") Session session
    ) {
        return ResponseEntity.ok(vehicleService.getById(id, session));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> update(
        @PathVariable Integer id,
        @Valid @RequestBody VehicleUpdateRequestDTO data,
        @RequestAttribute("session") Session session
    ) {
        return ResponseEntity.ok(vehicleService.update(id, data, session));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
        @PathVariable Integer id,
        @RequestAttribute("session") Session session
    ) {
        vehicleService.deactivate(id, session);
        return ResponseEntity.noContent().build();
    }
}
