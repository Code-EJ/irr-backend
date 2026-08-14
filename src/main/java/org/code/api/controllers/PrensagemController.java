package org.code.api.controllers;

import lombok.RequiredArgsConstructor;
import org.code.api.domain.prensagem.PrensagemRequestDTO;
import org.code.api.domain.prensagem.PrensagemResponseDTO;
import org.code.api.services.PrensagemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prensagens")
@RequiredArgsConstructor
public class PrensagemController {

    private final PrensagemService prensagemService;

    @PostMapping
    public ResponseEntity<PrensagemResponseDTO> create(@RequestBody PrensagemRequestDTO requestDTO) {
        PrensagemResponseDTO response = prensagemService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PrensagemResponseDTO>> listAll() {
        List<PrensagemResponseDTO> list = prensagemService.listAll();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrensagemResponseDTO> getById(@PathVariable UUID id) {
        PrensagemResponseDTO response = prensagemService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrensagemResponseDTO> update(
            @PathVariable UUID id,
            @RequestBody PrensagemRequestDTO requestDTO
    ) {
        PrensagemResponseDTO response = prensagemService.update(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        prensagemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
