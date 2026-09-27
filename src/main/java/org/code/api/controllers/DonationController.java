package org.code.api.controllers;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.domain.ports.DonationPort;
import org.code.api.dto.donation.request.DonationCreateRequestDTO;
import org.code.api.dto.donation.request.DonationUpdateRequestDTO;
import org.code.api.dto.donation.response.DonationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * HTTP boundary for organization-owned donation use cases; Swagger is the official contract.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/donations", "/api/v1/donations"})
public class DonationController {

  private final DonationPort donationPort;

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<DonationResponseDTO> create(
      @Valid @RequestBody DonationCreateRequestDTO data) {
    DonationResponseDTO response = donationPort.create(data);

    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Page<DonationResponseDTO>> list(
      @PageableDefault(size = 20, sort = "donationDate") Pageable pageable) {
    return ResponseEntity.ok(donationPort.list(pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<DonationResponseDTO> getById(@PathVariable UUID id) {
    return ResponseEntity.ok(donationPort.getById(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<DonationResponseDTO> update(
      @PathVariable UUID id, @Valid @RequestBody DonationUpdateRequestDTO data) {
    return ResponseEntity.ok(donationPort.update(id, data));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
    donationPort.deactivate(id);
    return ResponseEntity.noContent().build();
  }
}
