package org.code.api.intake.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.code.api.dto.collection.request.CollectionCreateRequestDTO;
import org.code.api.dto.collection.response.CollectionResponseDTO;
import org.code.api.intake.application.CollectionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes organization-owned collection CRUD with immutable consumed intake history.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@lombok.RequiredArgsConstructor
@RequestMapping("/api/v1/collections")
public class CollectionController {
  private final CollectionService service;

  /** Lists active collections with bounded pagination. */
  @GetMapping
  public Page<CollectionResponseDTO> list(
      @PageableDefault(size = 20, sort = "realizationDate") Pageable page) {
    return service.list(page);
  }

  /** Returns a collection and its input lines. */
  @GetMapping("/{id}")
  public CollectionResponseDTO get(@PathVariable UUID id) {
    return service.get(id);
  }

  /** Creates raw intake, without a stock credit. */
  @PostMapping
  public ResponseEntity<CollectionResponseDTO> create(
      @Valid @RequestBody CollectionCreateRequestDTO request) {
    var result = service.create(request);
    return ResponseEntity.created(URI.create("/api/v1/collections/" + result.id())).body(result);
  }

  /** Replaces an unprocessed collection. */
  @PutMapping("/{id}")
  public CollectionResponseDTO update(
      @PathVariable UUID id, @Valid @RequestBody CollectionCreateRequestDTO request) {
    return service.update(id, request);
  }

  /** Deactivates an unprocessed collection. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.deactivate(id);
    return ResponseEntity.noContent().build();
  }
}
