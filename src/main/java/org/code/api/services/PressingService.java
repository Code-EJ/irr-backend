package org.code.api.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.enums.OperationType;
import org.code.api.domain.exception.MaterialError;
import org.code.api.domain.exception.PressingError;
import org.code.api.domain.models.inventory.InventoryBalance;
import org.code.api.domain.models.inventory.InventoryLog;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serviço para gestão do processo de Prensagem de Materiais.
 *
 * <p>Regras de negócio e ACID:</p>
 * <ul>
 *   <li>Registro de fardos prensados com a transformação de volume (compactação)</li>
 *   <li>Atualização automática do volume ocupado no estoque (InventoryBalance)</li>
 *   <li>Registro de movimentação no livro razão de inventário (InventoryLog com PRESSING_OUTPUT)</li>
 *   <li>Garantia transacional de integridade (rollback total em caso de erro)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PressingService implements PressingPort {

    private final PressingRepository pressingRepository;
    private final PressedBaleRepository pressedBaleRepository;
    private final SortedItemRepository sortedItemRepository;
    private final MaterialSubtypeRepository subtypeRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider userProvider;

    @Override
    @Transactional
    public PressingResponseDTO create(PressingCreateRequestDTO data) {
        UUID userId = userProvider.getCurrentUserId();
        User creator = userRepository.getReferenceById(userId);

        OffsetDateTime date = data.pressingDate() != null ? data.pressingDate() : OffsetDateTime.now();

        Pressing pressing = pressingRepository.save(
            Pressing.builder()
                .pressingDate(date)
                .isActive(true)
                .creator(creator)
                .build()
        );

        List<PressedBale> savedBales = new ArrayList<>();

        if (data.pressedBales() != null) {
            for (PressedBaleRequestDTO baleDto : data.pressedBales()) {
                MaterialSubtype subtype = subtypeRepository.findById(baleDto.materialSubtypeId())
                    .filter(MaterialSubtype::getIsActive)
                    .orElseThrow(() -> new MaterialError.NotFound(baleDto.materialSubtypeId(), "SUBTYPE"));

                SortedItem sortedItem = null;
                if (baleDto.sortedItemId() != null) {
                    sortedItem = sortedItemRepository.findById(baleDto.sortedItemId())
                        .orElseThrow(() -> new PressingError.SortedItemNotFound(baleDto.sortedItemId()));
                }

                PressedBale bale = PressedBale.builder()
                    .pressing(pressing)
                    .sortedItem(sortedItem)
                    .materialSubtype(subtype)
                    .weightKg(baleDto.weightKg())
                    .initialVolumeM3(baleDto.initialVolumeM3())
                    .finalVolumeM3(baleDto.finalVolumeM3())
                    .destinationType(baleDto.destinationType())
                    .destinationId(baleDto.destinationId())
                    .isActive(true)
                    .build();

                bale = pressedBaleRepository.save(bale);
                savedBales.add(bale);

                // 1. Atualizar volume de compacto no saldo real do estoque (InventoryBalance)
                // Delta de volume = finalVolumeM3 - initialVolumeM3 (compactação reduz o espaço ocupado)
                BigDecimal volumeDelta = baleDto.finalVolumeM3().subtract(baleDto.initialVolumeM3());

                InventoryBalance balance = inventoryBalanceRepository.findByMaterialSubtypeId(subtype.getId())
                    .orElseGet(() -> InventoryBalance.builder()
                        .materialSubtype(subtype)
                        .currentWeightKg(BigDecimal.ZERO)
                        .currentVolumeM3(BigDecimal.ZERO)
                        .build());

                BigDecimal newVolume = balance.getCurrentVolumeM3().add(volumeDelta);
                if (newVolume.compareTo(BigDecimal.ZERO) < 0) {
                    newVolume = BigDecimal.ZERO;
                }

                balance.setCurrentVolumeM3(newVolume);
                inventoryBalanceRepository.save(balance);

                // 2. Registrar log de movimentação no livro razão de inventário
                InventoryLog inventoryLog = InventoryLog.builder()
                    .materialSubtype(subtype)
                    .quantityKg(baleDto.weightKg())
                    .quantityM3(volumeDelta)
                    .operationType(OperationType.PRESSING_OUTPUT)
                    .isActive(true)
                    .build();

                inventoryLogRepository.save(inventoryLog);
            }
        }

        pressing.setPressedBales(savedBales);
        log.info("Pressing record created successfully with ID: {} and {} bales by user {}", pressing.getId(), savedBales.size(), userId);
        return toResponse(pressing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PressingResponseDTO> list(Pageable pageable) {
        UUID userId = userProvider.getCurrentUserId();
        Page<Pressing> page = pressingRepository.findAllByCreatorId(userId, pageable);
        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PressingResponseDTO getById(UUID id) {
        UUID userId = userProvider.getCurrentUserId();

        Pressing pressing = pressingRepository.findByIdAndCreatorId(id, userId)
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
            pressing.getUpdatedAt()
        );
    }
}
