package org.code.api.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.exception.MaterialError;
import org.code.api.domain.exception.PressingError;
import org.code.api.domain.models.material.MaterialSubtype;
import org.code.api.domain.models.pressing.PressedBale;
import org.code.api.domain.models.pressing.Pressing;
import org.code.api.domain.models.sorting.SortedItem;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.domain.ports.PressingPort;
import org.code.api.dto.pressing.request.PressedBaleRequestDTO;
import org.code.api.dto.pressing.request.PressingCreateRequestDTO;
import org.code.api.dto.pressing.response.PressedBaleResponseDTO;
import org.code.api.dto.pressing.response.PressingResponseDTO;
import org.code.api.infrastructure.repositories.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Posts organization-owned pressing with conserved quantities and traceable stock lots.
 * Business effects and idempotency receipts commit in one transaction.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Slf4j
@Service
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class PressingService implements PressingPort {

    private final PressingRepository pressingRepository;
    private final PressedBaleRepository pressedBaleRepository;
    private final SortedItemRepository sortedItemRepository;
    private final MaterialSubtypeRepository subtypeRepository;
    private final org.code.api.inventory.application.StockLedger ledger;
    private final org.code.api.inventory.application.IdempotentCommands commands;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final jakarta.persistence.EntityManager entityManager;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider userProvider;
    private final org.code.api.domain.ports.OrganizationScope scope;

    @Override
    @Transactional
    public PressingResponseDTO create(PressingCreateRequestDTO data) {
        return commands.execute("CREATE_PRESSING", data, PressingResponseDTO.class, () -> post(data));
    }

    /** Posts a validated command after the organization lock and replay check. */
    private PressingResponseDTO post(PressingCreateRequestDTO data) {
        UUID organizationId = scope.organizationId();
        User creator = userRepository.getReferenceById(userProvider.getCurrentUserId());

        OffsetDateTime date = data.pressingDate() != null ? data.pressingDate() : OffsetDateTime.now();

        Pressing pressing = pressingRepository.save(
            Pressing.builder()
                .pressingDate(date)
                .isActive(true)
                .creator(creator).organizationId(organizationId)
                .build()
        );

        entityManager.flush();
        UUID operation = ledger.begin("PRESSING", pressing.getId(), date);

        List<PressedBale> savedBales = new ArrayList<>();

        if (data.pressedBales() != null) {
            for (PressedBaleRequestDTO baleDto : data.pressedBales()) {
                if (baleDto.finalVolumeM3().compareTo(baleDto.initialVolumeM3()) >= 0) {
                    throw new PressingError.InvalidCompaction(baleDto.initialVolumeM3(), baleDto.finalVolumeM3());
                }

                MaterialSubtype subtype = subtypeRepository.findByIdAndOrganizationId(baleDto.materialSubtypeId(), organizationId)
                    .filter(MaterialSubtype::getIsActive)
                    .orElseThrow(() -> new MaterialError.NotFound(baleDto.materialSubtypeId(), "SUBTYPE"));

                SortedItem sortedItem = sortedItemRepository.findByIdAndOrganizationId(baleDto.sortedItemId(), organizationId)
                    .orElseThrow(() -> new PressingError.SortedItemNotFound(baleDto.sortedItemId()));
                if (!sortedItem.getMaterialSubtype().getId().equals(subtype.getId())) {
                    throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Pressing must preserve the source material subtype");
                }
                if (baleDto.destinationId() != null || (baleDto.destinationType() != null
                        && baleDto.destinationType() != org.code.api.domain.enums.DestinationType.STOCK)) {
                    throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Pressing creates stock; use a separate sale command to sell it");
                }
                var lots = jdbc.queryForList("SELECT id FROM stock_lot WHERE organization_id=? AND sorted_item_id=? AND is_active", UUID.class, organizationId, sortedItem.getId());
                if (lots.isEmpty()) {
                    throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                        "The sorting output has no available stock lot");
                }
                ledger.consume(lots.getFirst(), subtype.getId(), baleDto.weightKg(), baleDto.initialVolumeM3());

                PressedBale bale = PressedBale.builder()
                    .pressing(pressing)
                    .sortedItem(sortedItem)
                    .materialSubtype(subtype).organizationId(organizationId)
                    .weightKg(baleDto.weightKg())
                    .initialVolumeM3(baleDto.initialVolumeM3())
                    .finalVolumeM3(baleDto.finalVolumeM3())
                    .destinationType(org.code.api.domain.enums.DestinationType.STOCK)
                    .destinationId(baleDto.destinationId())
                    .isActive(true)
                    .build();

                bale = pressedBaleRepository.save(bale);
                savedBales.add(bale);

                entityManager.flush();
                BigDecimal volumeDelta = baleDto.finalVolumeM3().subtract(baleDto.initialVolumeM3());
                ledger.move(operation, subtype.getId(), BigDecimal.ZERO, volumeDelta);
                ledger.lot(subtype.getId(), null, bale.getId(), baleDto.weightKg(), baleDto.finalVolumeM3());
            }
        }

        pressing.setPressedBales(savedBales);
        log.info("Pressing record created successfully with ID: {} and {} bales in organization {}", pressing.getId(), savedBales.size(), organizationId);
        return toResponse(pressing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PressingResponseDTO> list(Pageable pageable) {
        UUID organizationId = scope.organizationId();
        Page<Pressing> page = pressingRepository.findAllByOrganizationId(organizationId, pageable);
        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PressingResponseDTO getById(UUID id) {
        UUID organizationId = scope.organizationId();

        Pressing pressing = pressingRepository.findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new PressingError.NotFound(id));

        return toResponse(pressing);
    }

    private PressingResponseDTO toResponse(Pressing pressing) {
        List<PressedBaleResponseDTO> baleDTOs = pressing.getPressedBales() == null ? List.of() :
            pressing.getPressedBales().stream().map(bale -> new PressedBaleResponseDTO(
                bale.getId(),
                pressing.getId(),
                bale.getSortedItem() != null ? bale.getSortedItem().getId() : null,
                bale.getMaterialSubtype().getId(),
                bale.getWeightKg(),
                bale.getInitialVolumeM3(),
                bale.getFinalVolumeM3(),
                bale.getIsActive(),
                bale.getCreatedAt(),
                bale.getUpdatedAt(),
                bale.getDestinationType(),
                bale.getDestinationId()
            )).toList();

        return new PressingResponseDTO(
            pressing.getId(),
            pressing.getPressingDate(),
            pressing.getIsActive(),
            baleDTOs,
            pressing.getCreatedAt(),
            pressing.getUpdatedAt(),
            pressing.getStatus()
        );
    }
}
