package org.code.api.controllers;

import java.util.List;

import org.code.api.dto.collection.request.ColetaRequestDTO;
import org.code.api.dto.collection.response.ColetaResponseDTO;
import org.code.api.services.ColetaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coletas")
public class ColetaController {

    private final ColetaService coletaService;

    public ColetaController(ColetaService coletaService) {
        this.coletaService = coletaService;
    }

    @PostMapping
    public ResponseEntity<ColetaResponseDTO> criar(@RequestBody ColetaRequestDTO dto) {
        return ResponseEntity.ok(coletaService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<ColetaResponseDTO>> listar() {
        return ResponseEntity.ok(coletaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ColetaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(coletaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ColetaResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody ColetaRequestDTO dto
    ) {
        return ResponseEntity.ok(coletaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        coletaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}