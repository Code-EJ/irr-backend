package org.code.api.routes.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.routes.application.RouteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes scoped reusable planning, version preconditions and retained history.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/routes")
@Tag(name = "Routes")
public class RouteController {
  private final RouteService service;

  /** Lists active plans, optionally including retained inactive plans. */
  @GetMapping
  public Page<RouteContract.Response> list(
      @RequestParam(defaultValue = "false") boolean includeInactive, Pageable page) {
    return service.list(includeInactive, page);
  }

  /** Reads a scoped plan. */
  @GetMapping("/{id}")
  public RouteContract.Response get(@PathVariable UUID id) {
    return service.get(id);
  }

  /** Creates a complete reusable plan. */
  @PostMapping
  public ResponseEntity<RouteContract.Response> create(
      @Valid @RequestBody RouteContract.Request request) {
    var result = service.create(request);
    return ResponseEntity.created(URI.create("/api/v1/routes/" + result.id())).body(result);
  }

  /** Replaces current planning with the supplied version precondition. */
  @PutMapping("/{id}")
  public RouteContract.Response update(
      @PathVariable UUID id, @Valid @RequestBody RouteContract.Request request) {
    return service.update(id, request);
  }

  /** Soft-deactivates an owned plan once all open executions are resolved. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deactivate(@PathVariable UUID id, @RequestParam Long version) {
    service.deactivate(id, version);
    return ResponseEntity.noContent().build();
  }

  /** Lists transactional history without allowing mutation. */
  @GetMapping("/{id}/events")
  public Page<RouteContract.Event> events(@PathVariable UUID id, Pageable page) {
    return service.events(id, page);
  }
}
