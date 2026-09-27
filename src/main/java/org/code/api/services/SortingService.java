package org.code.api.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.enums.SortingType;
import org.code.api.domain.exception.MaterialError;
import org.code.api.domain.exception.SortingError;
import org.code.api.domain.models.collection.InputItem;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posts organization-owned sorting with conserved quantities and traceable stock lots. Business
 * effects and idempotency receipts commit in one transaction.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Slf4j
@Service
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SortingService implements SortingPort {

  private final SortingRepository sortingRepository;
  private final SortedItemRepository sortedItemRepository;
  private final MaterialSubtypeRepository subtypeRepository;
  private final InputItemRepository inputItemRepository;
  private final org.code.api.inventory.application.StockLedger ledger;
  private final org.code.api.inventory.application.IdempotentCommands commands;
  private final org.springframework.jdbc.core.JdbcTemplate jdbc;
  private final jakarta.persistence.EntityManager entityManager;
  private final UserRepository userRepository;
  private final AuthenticatedUserProvider userProvider;
  private final org.code.api.domain.ports.OrganizationScope scope;

  @Override
  @Transactional
  public SortingResponseDTO create(SortingCreateRequestDTO data) {
    return commands.execute("CREATE_SORTING", data, SortingResponseDTO.class, () -> post(data));
  }

  /** Posts a validated command after the organization lock and replay check. */
  private SortingResponseDTO post(SortingCreateRequestDTO data) {
    UUID organizationId = scope.organizationId();
    User creator = userRepository.getReferenceById(userProvider.getCurrentUserId());

    OffsetDateTime date = data.sortingDate() != null ? data.sortingDate() : OffsetDateTime.now();

    Sorting sorting =
        sortingRepository.save(
            Sorting.builder()
                .sortingDate(date)
                .sortingType(data.sortingType())
                .isActive(true)
                .creator(creator)
                .organizationId(organizationId)
                .build());

    entityManager.flush();
    UUID operation = ledger.begin("SORTING", sorting.getId(), date);

    List<SortedItem> savedItems = new ArrayList<>();

    if (data.sortedItems() != null) {
      for (SortedItemRequestDTO itemDto : data.sortedItems()) {
        MaterialSubtype subtype =
            subtypeRepository
                .findByIdAndOrganizationId(itemDto.materialSubtypeId(), organizationId)
                .filter(MaterialSubtype::getIsActive)
                .orElseThrow(
                    () -> new MaterialError.NotFound(itemDto.materialSubtypeId(), "SUBTYPE"));

        InputItem inputItem =
            inputItemRepository
                .findByIdAndOrganizationId(itemDto.inputItemId(), organizationId)
                .orElseThrow(() -> new SortingError.InputItemNotFound(itemDto.inputItemId()));
        if (itemDto.destinationId() != null
            || (itemDto.destinationType() != null
                && itemDto.destinationType() != org.code.api.domain.enums.DestinationType.STOCK)) {
          throw new org.springframework.web.server.ResponseStatusException(
              org.springframework.http.HttpStatus.BAD_REQUEST,
              "Sorting creates stock; use a separate pressing or sale command for subsequent"
                  + " destinations");
        }
        entityManager.flush();
        var consumed =
            jdbc.queryForMap(
                "SELECT COALESCE(SUM(weight_kg),0) AS weight, COALESCE(SUM(volume_m3),0) AS volume"
                    + " FROM sorted_item WHERE organization_id=? AND input_item_id=? AND is_active"
                    + " AND sorting_id IN (SELECT id FROM sorting WHERE status='POSTED')",
                organizationId,
                inputItem.getId());
        if (((BigDecimal) consumed.get("weight"))
                    .add(itemDto.weightKg())
                    .compareTo(inputItem.getWeightKg())
                > 0
            || ((BigDecimal) consumed.get("volume"))
                    .add(itemDto.volumeM3())
                    .compareTo(inputItem.getVolumeM3())
                > 0) {
          throw new org.springframework.web.server.ResponseStatusException(
              org.springframework.http.HttpStatus.CONFLICT,
              "Sorting exceeds the unprocessed input quantity");
        }

        BigDecimal rejectWeight =
            itemDto.rejectWeightKg() != null ? itemDto.rejectWeightKg() : BigDecimal.ZERO;
        BigDecimal rejectVolume =
            itemDto.rejectVolumeM3() != null ? itemDto.rejectVolumeM3() : BigDecimal.ZERO;

        SortedItem sortedItem =
            SortedItem.builder()
                .sorting(sorting)
                .inputItem(inputItem)
                .materialSubtype(subtype)
                .organizationId(organizationId)
                .weightKg(itemDto.weightKg())
                .volumeM3(itemDto.volumeM3())
                .rejectWeightKg(rejectWeight)
                .rejectVolumeM3(rejectVolume)
                .destinationType(org.code.api.domain.enums.DestinationType.STOCK)
                .destinationId(itemDto.destinationId())
                .isActive(true)
                .build();

        sortedItem = sortedItemRepository.save(sortedItem);
        savedItems.add(sortedItem);

        BigDecimal netWeight = itemDto.weightKg().subtract(rejectWeight);
        BigDecimal netVolume = itemDto.volumeM3().subtract(rejectVolume);

        entityManager.flush();
        ledger.move(operation, subtype.getId(), netWeight, netVolume);
        ledger.lot(subtype.getId(), sortedItem.getId(), null, netWeight, netVolume);
      }
    }

    sorting.setSortedItems(savedItems);
    log.info(
        "Sorting record created successfully with ID: {} and {} items in organization {}",
        sorting.getId(),
        savedItems.size(),
        organizationId);
    return toResponse(sorting);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<SortingResponseDTO> list(SortingType sortingType, Pageable pageable) {
    UUID organizationId = scope.organizationId();

    Page<Sorting> page;
    if (sortingType != null) {
      page =
          sortingRepository.findAllByOrganizationIdAndSortingType(
              organizationId, sortingType, pageable);
    } else {
      page = sortingRepository.findAllByOrganizationId(organizationId, pageable);
    }

    return page.map(this::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public SortingResponseDTO getById(UUID id) {
    UUID organizationId = scope.organizationId();

    Sorting sorting =
        sortingRepository
            .findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new SortingError.NotFound(id));

    return toResponse(sorting);
  }

  private SortingResponseDTO toResponse(Sorting sorting) {
    List<SortedItemResponseDTO> itemDTOs =
        sorting.getSortedItems() == null
            ? List.of()
            : sorting.getSortedItems().stream()
                .map(
                    item ->
                        new SortedItemResponseDTO(
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
                            item.getDestinationId()))
                .toList();

    return new SortingResponseDTO(
        sorting.getId(),
        sorting.getSortingDate(),
        sorting.getSortingType(),
        sorting.getIsActive(),
        itemDTOs,
        sorting.getCreatedAt(),
        sorting.getUpdatedAt(),
        sorting.getStatus());
  }
}
