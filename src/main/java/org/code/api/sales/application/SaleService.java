package org.code.api.sales.application;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.code.api.domain.models.sale.Sale;
import org.code.api.domain.models.sale.SaleItem;
import org.code.api.domain.models.base.Attachment;
import org.code.api.domain.ports.OrganizationScope;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.infrastructure.repositories.*;
import org.code.api.inventory.application.IdempotentCommands;
import org.code.api.inventory.application.StockLedger;
import org.code.api.sales.api.SaleContract.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
/**
 * Maintains editable drafts and posts/reverses sales atomically with stock and command receipts.
 * Line totals use HALF_UP at two decimals; the header is the sum of rounded line totals.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service @lombok.RequiredArgsConstructor @PreAuthorize("isAuthenticated()")
public class SaleService {
    private final SaleRepository sales;
    private final SaleItemRepository items;
    private final BuyerRepository buyers;
    private final AttachmentRepository attachments;
    private final MaterialSubtypeRepository materials;
    private final UserRepository users;
    private final OrganizationScope scope;
    private final AuthenticatedUserProvider actor;
    private final IdempotentCommands commands;
    private final StockLedger ledger;
    private final JdbcTemplate jdbc;
    /** Lists active sale documents in the selected organization. */
    @Transactional(readOnly=true)
    public Page<Response> list(Pageable page) { return sales.findAllByOrganizationId(scope.organizationId(),page).map(this::response); }
    /** Returns one scoped sale and its active lines. */
    @Transactional(readOnly=true)
    public Response get(UUID id) { return response(owned(id,scope.organizationId())); }
    /** Creates an unposted draft without consuming stock. */
    @Transactional
    public Response create(Draft request) {
        UUID org=scope.organizationId();
        Sale sale=Sale.builder().organizationId(org).creator(users.getReferenceById(actor.getCurrentUserId())).isActive(true).build();
        apply(sale,request,org); sale=sales.saveAndFlush(sale); replaceItems(sale,request,org); return response(sale);
    }
    /** Replaces a draft after verifying its optimistic version. */
    @Transactional
    public Response update(UUID id,Draft request) {
        UUID org=scope.organizationId(); Sale sale=owned(id,org); draft(sale); version(sale,request.version());
        apply(sale,request,org); sale=sales.saveAndFlush(sale); replaceItems(sale,request,org); return response(sale);
    }
    /** Deactivates a draft; posted sales must use the audited reversal transition. */
    @Transactional
    public void deactivate(UUID id,Long expectedVersion) {
        Sale sale=owned(id,scope.organizationId()); draft(sale); version(sale,expectedVersion);
        sale.setIsActive(false); sales.saveAndFlush(sale);
    }
    /** Posts one draft exactly once with fiscal evidence and sufficient lot quantities. */
    @Transactional
    public Response post(UUID id,Transition request) {
        return commands.execute("POST_SALE",new Command(id,request.version()),Response.class,()->{
            UUID org=scope.organizationId(); Sale sale=owned(id,org); draft(sale); version(sale,request.version());
            if(sale.getNfeAttachment()==null) throw bad("An invoice attachment is required to post a sale");
            attachment(sale.getNfeAttachment().getId(),org);
            List<SaleItem> lines=items.findAllBySaleId(id);
            if(lines.isEmpty()) throw bad("A sale must have at least one item");
            UUID operation=ledger.begin("SALE",id,sale.getSaleDate());
            for(SaleItem line:lines) {
                ledger.consume(line.getStockLotId(),line.getMaterialSubtype().getId(),line.getWeightKg(),line.getVolumeM3());
                ledger.move(operation,line.getMaterialSubtype().getId(),line.getWeightKg().negate(),line.getVolumeM3().negate());
            }
            sale.setStatus("POSTED"); sales.saveAndFlush(sale); return response(sale);
        });
    }
    /** Restores sold quantities and posts compensation while retaining the original sale. */
    @Transactional
    public Response reverse(UUID id,Transition request) {
        return commands.execute("REVERSE_SALE",new Command(id,request.version()),Response.class,()->{
            UUID org=scope.organizationId(); Sale sale=owned(id,org); version(sale,request.version());
            if(!"POSTED".equals(sale.getStatus())) throw conflict("Only a posted sale can be reversed");
            for(SaleItem line:items.findAllBySaleId(id)) {
                int changed=jdbc.update("UPDATE stock_lot SET available_weight_kg=available_weight_kg+?,available_volume_m3=available_volume_m3+? WHERE organization_id=? AND id=? AND is_active AND available_weight_kg+?<=original_weight_kg AND available_volume_m3+?<=original_volume_m3",line.getWeightKg(),line.getVolumeM3(),org,line.getStockLotId(),line.getWeightKg(),line.getVolumeM3());
                if(changed!=1) throw conflict("The source lot cannot accept the reversed quantities");
            }
            ledger.reverse("SALE",id); sale.setStatus("REVERSED"); sales.saveAndFlush(sale); return response(sale);
        });
    }
    private Sale owned(UUID id,UUID org) { return sales.findByIdAndOrganizationId(id,org).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Sale not found")); }
    private void draft(Sale sale) { if(!"DRAFT".equals(sale.getStatus())) throw conflict("Only draft sales can be edited or deactivated"); }
    private void version(Sale sale,Long expected) { if(expected==null) throw bad("The current sale version is required"); if(!expected.equals(sale.getVersion())) throw conflict("The sale changed; reload its current version"); }
    private void apply(Sale sale,Draft request,UUID org) {
        sale.setSaleDate(request.saleDate()); sale.setBuyer(buyers.findByIdAndOrganizationId(request.buyerId(),org).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Buyer not found")));
        sale.setNfeAttachment(attachment(request.nfeAttachmentId(),org)); sale.setMtrAttachment(attachment(request.mtrAttachmentId(),org)); sale.setCdfAttachment(attachment(request.cdfAttachmentId(),org));
        BigDecimal total=request.items().stream().map(i->money(i.weightKg(),i.unitPrice())).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(total.precision()-total.scale()>13) throw bad("Sale total exceeds supported precision");
        sale.setTotalValue(total); sale.setUpdatedAt(OffsetDateTime.now());
    }
    private void replaceItems(Sale sale,Draft request,UUID org) {
        var previous=items.findAllBySaleId(sale.getId()); previous.forEach(i->i.setIsActive(false)); items.saveAllAndFlush(previous);
        for(Line line:request.items()) {
            if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM stock_lot WHERE id=? AND organization_id=? AND material_subtype_id=? AND is_active)",Boolean.class,line.stockLotId(),org,line.materialSubtypeId())))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Stock lot not found for the selected material");
            var material=materials.findByIdAndOrganizationId(line.materialSubtypeId(),org).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Material subtype not found"));
            items.save(SaleItem.builder().sale(sale).organizationId(org).stockLotId(line.stockLotId()).materialSubtype(material).weightKg(line.weightKg()).volumeM3(line.volumeM3()).unitPrice(line.unitPrice()).isActive(true).build());
        }
        items.flush();
    }
    private Attachment attachment(UUID id,UUID org) { return id==null?null:attachments.findByIdAndOrganizationId(id,org).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Attachment not found")); }
    private BigDecimal money(BigDecimal weight,BigDecimal price) { return weight.multiply(price).setScale(2,RoundingMode.HALF_UP); }
    private Response response(Sale s) {
        var lines=items.findAllBySaleId(s.getId()).stream().map(i->new Item(i.getId(),i.getStockLotId(),i.getMaterialSubtype().getId(),i.getWeightKg(),i.getVolumeM3(),i.getUnitPrice(),money(i.getWeightKg(),i.getUnitPrice()))).toList();
        return new Response(s.getId(),s.getSaleDate(),s.getBuyer().getId(),s.getNfeAttachment()==null?null:s.getNfeAttachment().getId(),s.getMtrAttachment()==null?null:s.getMtrAttachment().getId(),s.getCdfAttachment()==null?null:s.getCdfAttachment().getId(),s.getStatus(),s.getCurrency(),s.getTotalValue(),s.getVersion(),lines,s.getCreatedAt(),s.getUpdatedAt());
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
}
