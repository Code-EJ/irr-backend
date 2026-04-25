package org.code.api.dto.logistic.vehicle.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Representação de leitura de um veículo do sistema")
public record VehicleResponseDTO(
        @Schema(description = "Identificador único do veículo", example = "1")
        Integer id,

        @Schema(description = "Placa do veículo", example = "ABC1D23")
        String placa,

        @Schema(description = "Modelo do veículo", example = "Toyota Corolla")
        String modelo,

        @Schema(description = "Indica se o veículo está ativo para uso na frota", example = "true")
        boolean ativo,


        @Schema(description = "Data de Criação do Veículo no sistema")
        LocalDateTime createdAt,

        @Schema(description = "Data da última atualização do Veículo no sistema")
        LocalDateTime updatedAt,

        @Schema(description = "Identificador único do usuário que criou o veículo no sistema", example = "550e8400-e29b-41d4-a716-446655440000")
        String createdById
) {}
