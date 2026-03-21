package org.code.api.dto.logistic.vehicle.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VehicleUpdateRequestDTO(
    @NotBlank(message = "Placa é um campo obrigatório")
    @Size(max = 191, message = "Placa deve ter no máximo 191 caracteres")
    String placa,
    @NotBlank(message = "Modelo é um campo obrigatório")
    @Size(max = 191, message = "Modelo deve ter no máximo 191 caracteres")
    String modelo,
    @NotNull(message = "Ativo é um campo obrigatório")
    Boolean ativo
) {}
