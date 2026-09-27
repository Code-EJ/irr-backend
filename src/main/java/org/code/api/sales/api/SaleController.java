package org.code.api.sales.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.code.api.sales.api.SaleContract.*;
import org.code.api.sales.application.SaleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Official sale API: draft CRUD followed by idempotent posting and audited reversal.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@lombok.RequiredArgsConstructor
@RequestMapping("/api/v1/sales")
public class SaleController {
  private final SaleService service;

  /** Lists the organization's active sales. */
  @GetMapping
  public Page<Response> list(@PageableDefault(size = 20, sort = "saleDate") Pageable page) {
    return service.list(page);
  }

  /** Returns a scoped sale. */
  @GetMapping("/{id}")
  public Response get(@PathVariable UUID id) {
    return service.get(id);
  }

  /** Creates an editable draft without reserving stock. */
  @PostMapping
  public ResponseEntity<Response> create(@Valid @RequestBody Draft request) {
    var r = service.create(request);
    return ResponseEntity.created(URI.create("/api/v1/sales/" + r.id())).body(r);
  }

  /** Replaces a draft using the supplied optimistic version. */
  @PutMapping("/{id}")
  public Response update(@PathVariable UUID id, @Valid @RequestBody Draft request) {
    return service.update(id, request);
  }

  /** Deactivates a draft using its current version. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam Long version) {
    service.deactivate(id, version);
    return ResponseEntity.noContent().build();
  }

  /** Posts fiscal evidence and stock effects with an Idempotency-Key. */
  @PostMapping("/{id}/post")
  public Response post(@PathVariable UUID id, @Valid @RequestBody Transition request) {
    return service.post(id, request);
  }

  /** Posts a compensating reversal with an Idempotency-Key. */
  @PostMapping("/{id}/reverse")
  public Response reverse(@PathVariable UUID id, @Valid @RequestBody Transition request) {
    return service.reverse(id, request);
  }
}
