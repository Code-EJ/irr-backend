package org.code.api.dto.pressing.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.time.OffsetDateTime;
import java.util.List;

public record PressingCreateRequestDTO(
    OffsetDateTime pressingDate,
    @NotEmpty(message = "This list must not be empty")
    @Valid
    List<PressedBaleRequestDTO> pressedBales
) {}
