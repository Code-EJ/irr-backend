package org.code.api.dto.sorting.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.code.api.domain.enums.SortingType;

import java.time.OffsetDateTime;
import java.util.List;

public record SortingCreateRequestDTO(
    OffsetDateTime sortingDate,
    @NotNull(message = "Sorting type is required")
    SortingType sortingType,
    @NotEmpty(message = "The list must have a sorted item")
    @Valid
    List<SortedItemRequestDTO> sortedItems
) {}
