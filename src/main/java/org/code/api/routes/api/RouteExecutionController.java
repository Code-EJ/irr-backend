package org.code.api.routes.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.routes.application.RouteExecutionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes dated assignments and explicit lifecycle commands without deprecated aliases.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/route-executions")
@Tag(name = "Route executions")
public class RouteExecutionController {
  private final RouteExecutionService service;

  /** Lists scoped history with optional route and status filters. */
  @GetMapping
  public Page<RouteExecutionContract.Response> list(
      @RequestParam(required = false) UUID routeId,
      @RequestParam(required = false) RouteExecutionContract.ExecutionStatus status,
      Pageable page) {
    return service.list(routeId, status, page);
  }

  /** Reads one preserved execution. */
  @GetMapping("/{id}")
  public RouteExecutionContract.Response get(@PathVariable UUID id) {
    return service.get(id);
  }

  /** Schedules a current active plan using existing fleet and staff. */
  @PostMapping
  public ResponseEntity<RouteExecutionContract.Response> create(
      @Valid @RequestBody RouteExecutionContract.Request request) {
    var result = service.create(request);
    return ResponseEntity.created(URI.create("/api/v1/route-executions/" + result.id()))
        .body(result);
  }

  /** Records actual departure using the server clock. */
  @PostMapping("/{id}/start")
  public RouteExecutionContract.Response start(
      @PathVariable UUID id, @Valid @RequestBody RouteExecutionContract.VersionCommand command) {
    return service.start(id, command);
  }

  /** Records arrival and an optional manual distance. */
  @PostMapping("/{id}/complete")
  public RouteExecutionContract.Response complete(
      @PathVariable UUID id, @Valid @RequestBody RouteExecutionContract.CompletionCommand command) {
    return service.complete(id, command);
  }

  /** Cancels an open execution through management authority. */
  @PostMapping("/{id}/cancel")
  public RouteExecutionContract.Response cancel(
      @PathVariable UUID id,
      @Valid @RequestBody RouteExecutionContract.CancellationCommand command) {
    return service.cancel(id, command);
  }

  /** Records or corrects declared coverage while execution remains in progress. */
  @PutMapping("/{id}/areas/{areaId}")
  public RouteExecutionContract.Response coverage(
      @PathVariable UUID id,
      @PathVariable UUID areaId,
      @Valid @RequestBody RouteExecutionContract.CoverageCommand command) {
    return service.coverage(id, areaId, command);
  }
}
