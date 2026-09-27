package org.code.api.parties.api;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.code.api.parties.application.BuyerService;
import org.code.api.parties.api.BuyerContract.Request;
import org.code.api.parties.api.BuyerContract.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
/**
 * Organization-scoped buyers CRUD, documented through the generated OpenAPI contract.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@lombok.RequiredArgsConstructor
@RequestMapping("/api/v1/buyers")
public class BuyerController {
    private final BuyerService service;
    /** Returns a bounded page of active records. */
    @GetMapping @Operation(summary="List buyers")
    public Page<Response> list(@PageableDefault(size=20,sort="name") Pageable page) { return service.list(page); }
    /** Returns one scoped record. */
    @GetMapping("/{id}") @Operation(summary="Get buyer")
    public Response get(@PathVariable UUID id) { return service.get(id); }
    /** Creates reference data as an organization manager. */
    @PostMapping @Operation(summary="Create buyer")
    public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
        var result=service.create(request); return ResponseEntity.created(URI.create("/api/v1/buyers/"+result.id())).body(result);
    }
    /** Replaces editable fields as an organization manager. */
    @PutMapping("/{id}") @Operation(summary="Update buyer")
    public Response update(@PathVariable UUID id,@Valid @RequestBody Request request) { return service.update(id,request); }
    /** Deactivates an unreferenced record as an organization manager. */
    @DeleteMapping("/{id}") @Operation(summary="Deactivate buyer")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { service.deactivate(id); return ResponseEntity.noContent().build(); }
}
