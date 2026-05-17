package org.code.api.controllers;

import lombok.RequiredArgsConstructor;
import org.code.api.domain.donation.DonationRequestDTO;
import org.code.api.domain.donation.DonationResponseDTO;
import org.code.api.services.DonationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService service;

    @PostMapping
    public DonationResponseDTO create(@RequestBody DonationRequestDTO dto) {
        return service.create(dto);
    }

    @GetMapping
    public List<DonationResponseDTO> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public DonationResponseDTO findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public DonationResponseDTO update(@PathVariable Long id, @RequestBody DonationRequestDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
