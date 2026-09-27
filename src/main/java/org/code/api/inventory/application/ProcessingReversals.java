package org.code.api.inventory.application;
import java.math.BigDecimal;
import java.util.UUID;
import org.code.api.domain.ports.OrganizationScope;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
/**
 * Reverses eligible processing in reverse dependency order without hiding operational history.
 * Organization serialization prevents a concurrent sale from consuming an output being reversed.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service @lombok.RequiredArgsConstructor @PreAuthorize("isAuthenticated()")
public class ProcessingReversals {
    private final JdbcTemplate jdbc;
    private final OrganizationScope scope;
    private final StockLedger ledger;
    private final IdempotentCommands commands;
    /** Stable replay response for a compensating operation. */
    public record Result(UUID id,String kind,String status,UUID reversalOperationId) {}
    /**
     * Reverses an unconsumed sorting or pressing output; downstream consumption must be reversed first.
     * @param kind SORTING or PRESSING
     * @param id operational document identity
     * @return persisted compensation identity
     */
    @Transactional
    public Result reverse(String kind,UUID id) {
        if(!java.util.Set.of("SORTING","PRESSING").contains(kind)) throw new IllegalArgumentException("Unsupported processing kind");
        return commands.execute("REVERSE_"+kind,id,Result.class,()->{
            UUID org=scope.organizationId(); String table=kind.equals("SORTING")?"sorting":"pressing";
            var states=jdbc.queryForList("SELECT status FROM "+table+" WHERE id=? AND organization_id=? AND is_active",String.class,id,org);
            if(states.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Processing record not found");
            if(!states.getFirst().equals("POSTED")) throw conflict("Processing record was already reversed");
            String child=kind.equals("SORTING")?"sorted_item":"pressed_bale";
            String reference=kind.equals("SORTING")?"sorted_item_id":"pressed_bale_id";
            var lots=jdbc.queryForList("SELECT l.* FROM stock_lot l JOIN "+child+" c ON c.id=l."+reference+" WHERE c."+table+"_id=? AND l.organization_id=?",id,org);
            if(lots.isEmpty()) throw conflict("Legacy processing without a stock ledger cannot be reversed through this API");
            for(var lot:lots) {
                if(!Boolean.TRUE.equals(lot.get("is_active")) || ((BigDecimal)lot.get("available_weight_kg")).compareTo((BigDecimal)lot.get("original_weight_kg"))!=0 || ((BigDecimal)lot.get("available_volume_m3")).compareTo((BigDecimal)lot.get("original_volume_m3"))!=0)
                    throw conflict("Reverse downstream allocations before reversing this processing record");
            }
            if(kind.equals("PRESSING")) {
                var bales=jdbc.queryForList("SELECT sorted_item_id,weight_kg,initial_volume_m3 FROM pressed_bale WHERE pressing_id=? AND organization_id=?",id,org);
                for(var bale:bales) {
                    int restored=jdbc.update("UPDATE stock_lot SET available_weight_kg=available_weight_kg+?,available_volume_m3=available_volume_m3+? WHERE sorted_item_id=? AND organization_id=? AND is_active AND available_weight_kg+?<=original_weight_kg AND available_volume_m3+?<=original_volume_m3",bale.get("weight_kg"),bale.get("initial_volume_m3"),bale.get("sorted_item_id"),org,bale.get("weight_kg"),bale.get("initial_volume_m3"));
                    if(restored!=1) throw conflict("The original sorting lot cannot receive the reversed quantities");
                }
            }
            for(var lot:lots) jdbc.update("UPDATE stock_lot SET is_active=false,available_weight_kg=0,available_volume_m3=0 WHERE id=? AND organization_id=?",lot.get("id"),org);
            UUID operation=ledger.reverse(kind,id);
            jdbc.update("UPDATE "+table+" SET status='REVERSED',updated_at=CURRENT_TIMESTAMP WHERE id=? AND organization_id=?",id,org);
            return new Result(id,kind,"REVERSED",operation);
        });
    }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
}
