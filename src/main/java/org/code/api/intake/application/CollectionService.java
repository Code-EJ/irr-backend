package org.code.api.intake.application;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.code.api.domain.models.collection.Collection;
import org.code.api.domain.models.collection.InputItem;
import org.code.api.domain.models.base.Attachment;
import org.code.api.domain.models.base.TeamMember;
import org.code.api.domain.ports.OrganizationScope;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.dto.collection.request.CollectionCreateRequestDTO;
import org.code.api.dto.collection.response.CollectionResponseDTO;
import org.code.api.dto.collection.response.InputItemResponseDTO;
import org.code.api.infrastructure.repositories.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
/**
 * Records raw collections with scoped fleet, staff, evidence and material inputs.
 * Raw intake never changes saleable inventory, and consumed history cannot be replaced.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service @lombok.RequiredArgsConstructor @PreAuthorize("isAuthenticated()")
public class CollectionService {
    private final CollectionRepository records;
    private final InputItemRepository inputs;
    private final VehicleRepository vehicles;
    private final TeamMemberRepository team;
    private final AttachmentRepository attachments;
    private final MaterialSubtypeRepository materials;
    private final UserRepository users;
    private final OrganizationScope scope;
    private final AuthenticatedUserProvider actor;
    private final JdbcTemplate jdbc;
    /** Lists scoped active collections. */
    @Transactional(readOnly=true)
    public Page<CollectionResponseDTO> list(Pageable page) { return records.findAllByOrganizationId(scope.organizationId(),page).map(this::response); }
    /** Returns one collection with its raw inputs. */
    @Transactional(readOnly=true)
    public CollectionResponseDTO get(UUID id) { return response(owned(id,scope.organizationId())); }
    /** Creates a collection and validates every referenced organization-owned record. */
    @Transactional
    public CollectionResponseDTO create(CollectionCreateRequestDTO request) {
        UUID org=scope.organizationId();
        Collection record=Collection.builder().organizationId(org).creator(users.getReferenceById(actor.getCurrentUserId())).isActive(true).build();
        apply(record,request,org); records.saveAndFlush(record); replaceInputs(record,request,org);
        return response(record);
    }
    /** Replaces an unprocessed collection; posted processing history makes it immutable. */
    @Transactional
    public CollectionResponseDTO update(UUID id,CollectionCreateRequestDTO request) {
        UUID org=scope.organizationId(); Collection record=owned(id,org); requireUnprocessed(id,org);
        apply(record,request,org); records.saveAndFlush(record); replaceInputs(record,request,org);
        return response(record);
    }
    /** Deactivates an unprocessed collection and its raw inputs. */
    @Transactional
    public void deactivate(UUID id) {
        UUID org=scope.organizationId(); Collection record=owned(id,org); requireUnprocessed(id,org);
        record.setIsActive(false); records.saveAndFlush(record);
        var items=inputs.findAllByCollectionId(id); items.forEach(item->item.setIsActive(false)); inputs.saveAllAndFlush(items);
    }
    private Collection owned(UUID id,UUID org) { return records.findByIdAndOrganizationId(id,org).orElseThrow(()->missing("Collection")); }
    private void requireUnprocessed(UUID id,UUID org) {
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM sorted_item s JOIN input_item i ON i.id=s.input_item_id WHERE i.collection_id=? AND i.organization_id=?)",Boolean.class,id,org)))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Processed collections cannot be edited or deactivated");
    }
    private void apply(Collection record,CollectionCreateRequestDTO request,UUID org) {
        if(request.inputItems()==null || request.inputItems().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Input items are required");
        BigDecimal total=request.inputItems().stream().map(item->item.weightKg()).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(request.totalWeightKg()!=null && total.compareTo(request.totalWeightKg())!=0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Total weight must equal the sum of input items");
        record.setRouteDescription(request.routeDescription()); record.setDepartureAt(request.departureAt()); record.setArrivalAt(request.arrivalAt()); record.setDistanceKm(request.distanceKm());
        record.setRealizationDate(request.realizationDate()); record.setTotalWeightKg(total);
        record.setVehicle(vehicles.findByIdAndOrganizationId(request.vehicleId(),org).orElseThrow(()->missing("Vehicle")));
        TeamMember driver=member(request.driverId(),org);
        if(!"DRIVER".equals(driver.getRole())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selected team member must have the DRIVER role");
        record.setDriver(driver);
        record.setMtrGenerator(attachment(request.mtrGeneratorId(),org));
        record.setMtrDestinator(attachment(request.mtrDestinatorId(),org));
        record.setCollectionDiary(attachment(request.collectionDiaryId(),org));
        Set<UUID> ids=request.teamMemberIds()==null?Set.of():request.teamMemberIds();
        if(ids.contains(request.driverId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The driver is already assigned separately from the collection team");
        record.setTeamMembers(ids.stream().map(id->member(id,org)).collect(Collectors.toSet()));
    }
    private void replaceInputs(Collection record,CollectionCreateRequestDTO request,UUID org) {
        var previous=inputs.findAllByCollectionId(record.getId()); previous.forEach(item->item.setIsActive(false)); inputs.saveAllAndFlush(previous);
        var replacements=request.inputItems().stream().map(item->InputItem.builder().organizationId(org).collection(record)
            .materialSubtype(materials.findByIdAndOrganizationId(item.materialSubtypeId(),org).orElseThrow(()->missing("Material subtype")))
            .weightKg(item.weightKg()).volumeM3(item.volumeM3()).isActive(true).build()).toList();
        record.setInputItems(inputs.saveAllAndFlush(replacements));
    }
    private TeamMember member(UUID id,UUID org) { return team.findByIdAndOrganizationId(id,org).orElseThrow(()->missing("Team member")); }
    private Attachment attachment(UUID id,UUID org) { return id==null?null:attachments.findByIdAndOrganizationId(id,org).orElseThrow(()->missing("Attachment")); }
    private ResponseStatusException missing(String resource) { return new ResponseStatusException(HttpStatus.NOT_FOUND,resource+" not found"); }
    private CollectionResponseDTO response(Collection r) {
        List<InputItemResponseDTO> items=inputs.findAllByCollectionId(r.getId()).stream().map(i->new InputItemResponseDTO(i.getId(),r.getId(),null,i.getMaterialSubtype().getId(),i.getWeightKg(),i.getVolumeM3(),i.getIsActive(),i.getCreatedAt(),i.getUpdatedAt())).toList();
        return new CollectionResponseDTO(r.getId(),r.getRealizationDate(),r.getTotalWeightKg(),r.getVehicle().getId(),r.getDriver().getId(),r.getMtrGenerator()==null?null:r.getMtrGenerator().getId(),r.getMtrDestinator()==null?null:r.getMtrDestinator().getId(),r.getCollectionDiary()==null?null:r.getCollectionDiary().getId(),r.getIsActive(),r.getTeamMembers().stream().map(TeamMember::getId).collect(Collectors.toSet()),items,r.getCreatedAt(),r.getUpdatedAt(),r.getRouteDescription(),r.getDepartureAt(),r.getArrivalAt(),r.getDistanceKm());
    }
}
