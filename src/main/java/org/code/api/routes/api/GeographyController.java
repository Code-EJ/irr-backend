package org.code.api.routes.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.routes.application.GeographyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes stable shared territory references using the supported versioned contract.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Territory")
public class GeographyController {
  private final GeographyService service;

  /** Resolves a stable municipality for current paginated reference selections. */
  @GetMapping("/municipalities/{id}")
  public GeographyContract.MunicipalityResponse municipality(@PathVariable UUID id) {
    return service.municipality(id);
  }

  /** Resolves a stable neighborhood for current paginated reference selections. */
  @GetMapping("/neighborhoods/{id}")
  public GeographyContract.NeighborhoodResponse neighborhood(@PathVariable UUID id) {
    return service.neighborhood(id);
  }

  /** Lists municipalities for authenticated active organization users. */
  @GetMapping("/municipalities")
  public Page<GeographyContract.MunicipalityResponse> municipalities(Pageable page) {
    return service.municipalities(page);
  }

  /** Registers a municipality through global administrator authority only. */
  @PostMapping("/municipalities")
  public ResponseEntity<GeographyContract.MunicipalityResponse> municipality(
      @Valid @RequestBody GeographyContract.MunicipalityRequest request) {
    var result = service.createMunicipality(request);
    return ResponseEntity.created(
            URI.create("/api/v1/municipalities?ibgeCode=" + result.ibgeCode()))
        .body(result);
  }

  /** Lists neighborhoods belonging to one stable municipality. */
  @GetMapping("/neighborhoods")
  public Page<GeographyContract.NeighborhoodResponse> neighborhoods(
      @RequestParam UUID municipalityId, Pageable page) {
    return service.neighborhoods(municipalityId, page);
  }

  /** Registers a municipality-owned neighborhood through global administrator authority only. */
  @PostMapping("/neighborhoods")
  public ResponseEntity<GeographyContract.NeighborhoodResponse> neighborhood(
      @Valid @RequestBody GeographyContract.NeighborhoodRequest request) {
    var result = service.createNeighborhood(request);
    return ResponseEntity.created(
            URI.create("/api/v1/neighborhoods?municipalityId=" + result.municipalityId()))
        .body(result);
  }
}
