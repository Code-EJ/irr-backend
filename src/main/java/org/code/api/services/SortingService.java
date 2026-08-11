package org.code.api.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.enums.OperationType;
import org.code.api.domain.enums.SortingType;
import org.code.api.domain.exception.MaterialError;
import org.code.api.domain.exception.SortingError;
import org.code.api.domain.models.collection.InputItem;
import org.code.api.domain.models.inventory.InventoryBalance;
import org.code.api.domain.models.inventory.InventoryLog;
import org.code.api.domain.models.material.MaterialSubtype;
import org.code.api.domain.models.sorting.SortedItem;
import org.code.api.domain.models.sorting.Sorting;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.domain.ports.SortingPort;
import org.code.api.dto.sorting.request.SortedItemRequestDTO;
import org.code.api.dto.sorting.request.SortingCreateRequestDTO;
import org.code.api.dto.sorting.response.SortedItemResponseDTO;
import org.code.api.dto.sorting.response.SortingResponseDTO;
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
 * Serviço para gestão do processo de Triagem de Materiais.
 *
 * <p>Regras de negócio e ACID:</p>
 * <ul>
 *   <li>Separação de materiais por tipo/subtipo e rejeito</li>
 *   <li>Atualização automática do saldo real do estoque (InventoryBalance)</li>
 *   <li>Registro de movimentação no livro razão de inventário (InventoryLog com SORTING_OUTPUT)</li>
 *   <li>Documentação de perdas/rejeito por item triado</li>
 *   <li>Garantia transacional de integridade (rollback total em caso de erro)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SortingService implements SortingPort {

    private final SortingRepository sortingRepository;
    private final SortedItemRepository sortedItemRepository;
    private final MaterialSubtypeRepository subtypeRepository;
    private final InputItemRepository inputItemRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider userProvider;

    @Override
    @Transactional
    public SortingResponseDTO create(SortingCreateRequestDTO data) {
        UUID userId = userProvider.getCurrentUserId();
        User creator = userRepository.getReferenceById(userId);

        OffsetDateTime date = data.sortingDate() != null ? data.sortingDate() : OffsetDateTime.now();

        Sorting sorting = sortingRepository.save(
            Sorting.builder()
                .sortingDate(date)
                .sortingType(data.sortingType())
                .isActive(true)
                .creator(creator)
                .build()
        );

        List<SortedItem> savedItems = new ArrayList<>();

        if (data.sortedItems() != null) {
            for (SortedItemRequestDTO itemDto : data.sortedItems()) {
                MaterialSubtype subtype = subtypeRepository.findById(itemDto.materialSubtypeId())
                    .filter(MaterialSubtype::getIsActive)
                    .orElseThrow(() -> new MaterialError.NotFound(itemDto.materialSubtypeId(), "SUBTYPE"));

                InputItem inputItem = null;
                if (itemDto.inputItemId() != null) {
                    inputItem = inputItemRepository.findById(itemDto.inputItemId())
                        .orElseThrow(() -> new SortingError.InputItemNotFound(itemDto.inputItemId()));
                }

                BigDecimal rejectWeight = itemDto.rejectWeightKg() != null ? itemDto.rejectWeightKg() : BigDecimal.ZERO;
                BigDecimal rejectVolume = itemDto.rejectVolumeM3() != null ? itemDto.rejectVolumeM3() : BigDecimal.ZERO;

                SortedItem sortedItem = SortedItem.builder()
                    .sorting(sorting)
                    .inputItem(inputItem)
                    .materialSubtype(subtype)
                    .weightKg(itemDto.weightKg())
                    .volumeM3(itemDto.volumeM3())
                    .rejectWeightKg(rejectWeight)
                    .rejectVolumeM3(rejectVolume)
                    .destinationType(itemDto.destinationType())
                    .destinationId(itemDto.destinationId())
                    .isActive(true)
                    .build();

                sortedItem = sortedItemRepository.save(sortedItem);
                savedItems.add(sortedItem);

                // Massa líquida = Massa Bruta - Rejeito
                BigDecimal netWeight = itemDto.weightKg().subtract(rejectWeight);
                BigDecimal netVolume = itemDto.volumeM3().subtract(rejectVolume);

                // 1. Atualizar saldo real de estoque (InventoryBalance) com a massa LÍQUIDA
                InventoryBalance balance = inventoryBalanceRepository.findByMaterialSubtypeId(subtype.getId())
                    .orElseGet(() -> InventoryBalance.builder()
                        .materialSubtype(subtype)
                        .currentWeightKg(BigDecimal.ZERO)
                        .currentVolumeM3(BigDecimal.ZERO)
                        .build());

                balance.setCurrentWeightKg(balance.getCurrentWeightKg().add(netWeight));
                balance.setCurrentVolumeM3(balance.getCurrentVolumeM3().add(netVolume));
                inventoryBalanceRepository.save(balance);

                // 2. Registrar log do material aproveitado (entrada no saldo)
                inventoryLogRepository.save(InventoryLog.builder()
                    .materialSubtype(subtype)
                    .quantityKg(netWeight)
                    .quantityM3(netVolume)
                    .operationType(OperationType.SORTING_OUTPUT)
                    .isActive(true)
                    .build());

                // 3. Registrar log do rejeito (saída/descarte) — apenas se houver rejeito
                if (rejectWeight.signum() > 0 || rejectVolume.signum() > 0) {
                    inventoryLogRepository.save(InventoryLog.builder()
                        .materialSubtype(subtype)
                        .quantityKg(rejectWeight.negate())
                        .quantityM3(rejectVolume.negate())
                        .operationType(OperationType.MANUAL_ADJUSTMENT)
                        .isActive(true)
                        .build());
                }
            }
        }

        sorting.setSortedItems(savedItems);
        log.info("Sorting record created successfully with ID: {} and {} items by user {}", sorting.getId(), savedItems.size(), userId);
        return toResponse(sorting);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SortingResponseDTO> list(SortingType sortingType, Pageable pageable) {
        UUID userId = userProvider.getCurrentUserId();

        Page<Sorting> page;
        if (sortingType != null) {
            page = sortingRepository.findAllByCreatorIdAndSortingType(userId, sortingType, pageable);
        } else {
            page = sortingRepository.findAllByCreatorId(userId, pageable);
        }

        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SortingResponseDTO getById(UUID id) {
        UUID userId = userProvider.getCurrentUserId();

        Sorting sorting = sortingRepository.findByIdAndCreatorId(id, userId)
            .orElseThrow(() -> new SortingError.NotFound(id));

        return toResponse(sorting);
    }

    private SortingResponseDTO toResponse(Sorting sorting) {
        List<SortedItemResponseDTO> itemDTOs = sorting.getSortedItems() == null ? List.of() :
            sorting.getSortedItems().stream().map(item -> new SortedItemResponseDTO(
                item.getId(),
                sorting.getId(),
                item.getInputItem() != null ? item.getInputItem().getId() : null,
                item.getMaterialSubtype().getId(),
                item.getWeightKg(),
                item.getVolumeM3(),
                item.getRejectWeightKg(),
                item.getRejectVolumeM3(),
                item.getIsActive(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getDestinationType(),
                item.getDestinationId()
            )).toList();

        return new SortingResponseDTO(
            sorting.getId(),
            sorting.getSortingDate(),
            sorting.getSortingType(),
            sorting.getIsActive(),
            itemDTOs,
            sorting.getCreatedAt(),
            sorting.getUpdatedAt()
        );
    }
}
