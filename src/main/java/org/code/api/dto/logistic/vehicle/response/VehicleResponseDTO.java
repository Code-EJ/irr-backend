package org.code.api.dto.logistic.vehicle.response;

import java.time.LocalDateTime;

public record VehicleResponseDTO(
    Integer id,
    String placa,
    String modelo,
    Boolean ativo,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdById
) {}
