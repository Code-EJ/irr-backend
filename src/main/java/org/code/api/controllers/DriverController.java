package org.code.api.controllers;

import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import java.util.List;
import org.code.api.dto.driver.DriverRequestDTO;
import org.code.api.dto.driver.DriverResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.code.api.services.DriverService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;




@RestController
@RequestMapping("api/driver")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;
    
@PostMapping  // Create driver
    public ResponseEntity<DriverResponseDTO> create(@RequestBody DriverRequestDTO resquestDTO)  {
        DriverResponseDTO response = driverService.create(resquestDTO);
        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }
@GetMapping  // Get|Find driver
    public ResponseEntity<List<DriverResponseDTO>> findAll() {
        List<DriverResponseDTO> drivers = driverService.findAll();
        return ResponseEntity.ok(drivers);
    }
@GetMapping("/{id}") // Get by id
    public ResponseEntity<DriverResponseDTO> findById(@PathVariable Integer id) {
        DriverResponseDTO driver = driverService.findById(id);
        return ResponseEntity.ok(driver);
    }
@PutMapping("/{id}") // Update
    public ResponseEntity<DriverResponseDTO> update(
            @PathVariable Integer id,
            @RequestBody DriverRequestDTO requestDTO) {
        DriverResponseDTO driver = driverService.update(id, requestDTO);
        return ResponseEntity.ok(driver);
    }
@DeleteMapping("/{id}") // Delete
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        driverService.delete(id);
        return ResponseEntity.noContent().build();
    }



}
