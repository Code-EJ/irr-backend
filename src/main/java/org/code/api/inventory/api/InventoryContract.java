package org.code.api.inventory.api;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
/**
 * Read-only inventory projections and reconciliation evidence.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public final class InventoryContract {
    private InventoryContract() {}
    /** Current saleable quantities for one material. */
    public record Balance(UUID materialSubtypeId,String materialName,BigDecimal weightKg,BigDecimal volumeM3,long version) {}
    /** Traceable sorting or pressing output and its unallocated quantities. */
    public record Lot(UUID id,UUID materialSubtypeId,String materialName,UUID sortedItemId,UUID pressedBaleId,BigDecimal availableWeightKg,BigDecimal availableVolumeM3,OffsetDateTime createdAt) {}
    /** Immutable signed movement with business time and authenticated actor. */
    public record Movement(UUID id,UUID operationId,String kind,UUID materialSubtypeId,BigDecimal weightDeltaKg,BigDecimal volumeDeltaM3,OffsetDateTime occurredAt,UUID actorId) {}
    /** Differences are projection minus ledger, and projection minus available lots. */
    public record Reconciliation(UUID materialSubtypeId,BigDecimal ledgerWeightDifference,BigDecimal ledgerVolumeDifference,BigDecimal lotWeightDifference,BigDecimal lotVolumeDifference,boolean consistent) {}
}
