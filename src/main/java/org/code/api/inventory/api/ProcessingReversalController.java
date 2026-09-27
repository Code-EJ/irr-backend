package org.code.api.inventory.api;
import java.util.UUID;
import org.code.api.inventory.application.ProcessingReversals;
import org.springframework.web.bind.annotation.*;
/**
 * Explicit correction transitions for posted processing records; original documents remain readable.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController @lombok.RequiredArgsConstructor @RequestMapping("/api/v1")
public class ProcessingReversalController {
    private final ProcessingReversals reversals;
    /** Reverses sorting after all downstream allocations are restored. */
    @PostMapping("/sortings/{id}/reverse") public ProcessingReversals.Result sorting(@PathVariable UUID id) { return reversals.reverse("SORTING",id); }
    /** Restores sorted source quantities and reverses unconsumed pressed output. */
    @PostMapping("/pressings/{id}/reverse") public ProcessingReversals.Result pressing(@PathVariable UUID id) { return reversals.reverse("PRESSING",id); }
}
