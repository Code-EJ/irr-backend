package org.code.api.dto.logistic.vehicle.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) que encapsula o payload para atualização de um Veículo.
 *
 * <p>Implementado como um {@code record} para garantir imutabilidade e thread-safety
 * desde a desserialização do JSON até a camada de serviço. As validações estruturais
 * (Jakarta Bean Validation) devem ser processadas nesta camada antes de atingirem o domínio.
 *
 * @param placa A nova placa do veículo (deve respeitar a máscara e não pode existir no banco).
 * @param modelo A descrição ou nome do modelo do veículo atualizado.
 * @param ativo O status de disponibilidade do veículo na frota.
 *
 * @implNote Este DTO não carrega o ID do veículo, pois o identificador do recurso deve ser sempre extraído da URI (Path Variable) em obediência às boas práticas REST.
 */
public record VehicleUpdateRequestDTO(
    @Schema(description = "Nova placa do veículo (caso tenha mudado de estado/padrão)", example = "XYZ9E87")
    @NotBlank(message = "{api.vehicle.placa.notblank}")
    @Pattern(regexp = "^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$", message = "{api.vehicle.placa.pattern}")
    @Size(max=191, message = "{api.vehicle.placa.size}")
    String placa,

    @Schema(description = "Atualização do modelo do veículo", example = "Toyota Corolla 2024")
    @NotBlank(message = "{api.vehicle.modelo.notblank}")
    @Size(max = 191, message = "{api.vehicle.modelo.size}")
    String modelo,

    @NotNull(message = "O campo 'Ativo' é um obrigatório.")
    Boolean ativo
) {}
