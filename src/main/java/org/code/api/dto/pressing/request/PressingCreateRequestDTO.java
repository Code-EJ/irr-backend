package org.code.api.dto.pressing.request;

import jakarta.validation.Valid;

import java.time.OffsetDateTime;
import java.util.List;

public record PressingCreateRequestDTO(
    OffsetDateTime pressingDate,
    @Valid
    List<PressedBaleRequestDTO> pressedBales
) {}
