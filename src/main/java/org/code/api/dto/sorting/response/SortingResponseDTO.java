package org.code.api.dto.sorting.response;

import org.code.api.domain.enums.SortingType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SortingResponseDTO(
    UUID id,
    OffsetDateTime sortingDate,
    SortingType sortingType,
    Boolean isActive,
    List<SortedItemResponseDTO> sortedItems,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
