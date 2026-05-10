package org.code.api.dto.logistic.vehicle.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO para um item individual de atualização em massa.
 * Cada item identifica o veículo pelo ID e carrega os novos dados.
 */
public record VehicleBulkUpdateItemDTO(
    @NotNull(message = "Vehicle ID is required")
    UUID id,
    @NotBlank(message = "License plate is required")
    @Size(max = 20, message = "License plate must be at most 20 characters")
    String licensePlate,
    @Size(max = 100, message = "Model must be at most 100 characters")
    String model,
    @NotNull(message = "Active status is required")
    Boolean isActive
) {}
