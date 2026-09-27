package org.code.api.sales.api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * Sale lifecycle contracts; monetary totals are calculated by the server in BRL.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public final class SaleContract {
    private SaleContract() {}
    /** Draft line allocated to a typed lot; unit price is BRL per kilogram. */
    @Schema(name="SaleLineRequest")
    public record Line(@NotNull UUID stockLotId,@NotNull UUID materialSubtypeId,
        @NotNull @Positive @Digits(integer=11,fraction=4) BigDecimal weightKg,
        @NotNull @PositiveOrZero @Digits(integer=11,fraction=4) BigDecimal volumeM3,
        @NotNull @PositiveOrZero @Digits(integer=11,fraction=4) BigDecimal unitPrice) {}
    /** Draft fields; version is mandatory for replacements and omitted for creation. */
    @Schema(name="SaleDraftRequest")
    public record Draft(@NotNull OffsetDateTime saleDate,@NotNull UUID buyerId,
        UUID nfeAttachmentId,UUID mtrAttachmentId,UUID cdfAttachmentId,
        @NotEmpty @Size(max=200) List<@NotNull @Valid Line> items,@PositiveOrZero Long version) {}
    /** Optimistic version required for a state transition. */
    @Schema(name="SaleTransitionRequest")
    public record Transition(@NotNull @PositiveOrZero Long version) {}
    /** Includes the target in command hashing to prevent cross-sale receipt reuse. */
    public record Command(UUID id,Long version) {}
    /** Persisted line with server-calculated rounded monetary value. */
    @Schema(name="SaleLineResponse")
    public record Item(UUID id,UUID stockLotId,UUID materialSubtypeId,BigDecimal weightKg,BigDecimal volumeM3,BigDecimal unitPrice,BigDecimal lineTotal) {}
    /** Safe sale representation, including lifecycle and optimistic version. */
    @Schema(name="SaleResponse")
    public record Response(UUID id,OffsetDateTime saleDate,UUID buyerId,UUID nfeAttachmentId,UUID mtrAttachmentId,UUID cdfAttachmentId,
        String status,String currency,BigDecimal totalValue,Long version,List<Item> items,OffsetDateTime createdAt,OffsetDateTime updatedAt) {}
}
