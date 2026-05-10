package org.code.api.dto.logistic.vehicle.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO para atualização em massa de veículos.
 *
 * <p>Limita o batch a 100 itens por requisição.</p>
 */
public record VehicleBulkUpdateRequestDTO(
    @NotEmpty(message = "Vehicle list must not be empty")
    @Size(max = 100, message = "Cannot update more than 100 vehicles at once")
    @Valid
    List<VehicleBulkUpdateItemDTO> vehicles
) {}
