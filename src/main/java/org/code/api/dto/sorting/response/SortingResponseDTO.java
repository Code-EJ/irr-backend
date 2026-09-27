package org.code.api.dto.sorting.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.code.api.domain.enums.SortingType;

/**
 * Typed Sorting Response API value.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record SortingResponseDTO(
    UUID id,
    OffsetDateTime sortingDate,
    SortingType sortingType,
    Boolean isActive,
    List<SortedItemResponseDTO> sortedItems,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String status) {}
