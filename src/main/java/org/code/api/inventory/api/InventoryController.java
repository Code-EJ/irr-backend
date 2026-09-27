package org.code.api.inventory.api;
import java.util.List;
import java.util.UUID;
import org.code.api.inventory.application.InventoryQueries;
import org.code.api.inventory.api.InventoryContract.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
/**
 * Read-only inventory API; stock mutations use typed processing and sale commands.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController @lombok.RequiredArgsConstructor @RequestMapping("/api/v1/inventory")
public class InventoryController {
    private final InventoryQueries queries;
    /** Returns a bounded balance page. */
    @GetMapping("/balances") public Page<Balance> balances(@PageableDefault(size=20) Pageable page) { return queries.balances(page); }
    /** Returns traceable lots suitable for pressing and sale allocation. */
    @GetMapping("/lots") public Page<Lot> lots(@RequestParam(required=false) UUID materialSubtypeId,@PageableDefault(size=20) Pageable page) { return queries.lots(materialSubtypeId,page); }
    /** Returns immutable signed movements. */
    @GetMapping("/movements") public Page<Movement> movements(@PageableDefault(size=20) Pageable page) { return queries.movements(page); }
    /** Reports differences without automatically modifying authoritative data. */
    @GetMapping("/reconciliation") public List<Reconciliation> reconcile() { return queries.reconcile(); }
}
