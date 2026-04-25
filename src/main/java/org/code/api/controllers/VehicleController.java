package org.code.api.controllers;

import jakarta.validation.Valid;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.code.api.domain.ports.VehiclePort;
import org.code.api.dto.logistic.vehicle.request.VehicleCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.response.VehicleResponseDTO;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Controlador REST (Adaptador de Entrada) para a gestão de Veículos da frota.
 *
 * <p>Esta classe expõe as operações de CRUD da entidade Vehicle seguindo as diretrizes
 * de maturidade REST (Nível 2 de Richardson). Ela atua puramente como uma camada de
 * transporte (HTTP), delegando toda a lógica de negócio e segurança para a porta de
 * domínio {@link org.code.api.domain.ports.VehiclePort}.
 *
 * @implNote As respostas de sucesso em criação retornam HTTP 201 com o cabeçalho {@code Location}.
 * A proteção de rotas via AOP (ex: {@code @PreAuthorize}) e o tratamento de exceções
 * são gerenciados globalmente pelo framework (ControllerAdvice).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/veiculos")
public class VehicleController {

    private final VehiclePort vehiclePort;


    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping
    public ResponseEntity<VehicleResponseDTO> create(
        @Valid @RequestBody VehicleCreateRequestDTO data
    ) {
        VehicleResponseDTO vehicleResponseDTO = vehiclePort.create(data);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(vehicleResponseDTO.id())
                .toUri();

        return ResponseEntity.created(location).body(vehicleResponseDTO);
    }

    @GetMapping
    public ResponseEntity<Page<VehicleResponseDTO>> list(
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(vehiclePort.list(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> getById(
        @PathVariable Integer id
    ) {
        return ResponseEntity.ok(vehiclePort.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> update(
        @PathVariable Integer id,
        @Valid @RequestBody VehicleUpdateRequestDTO data
    ) {
        return ResponseEntity.ok(vehiclePort.update(id, data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
        @PathVariable Integer id
    ) {
        vehiclePort.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
