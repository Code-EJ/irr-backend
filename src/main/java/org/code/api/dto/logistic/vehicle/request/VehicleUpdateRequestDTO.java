package org.code.api.dto.logistic.vehicle.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * @implNote Este DTO não carrega o ID do veículo, pois o identificador do recurso deve ser
 * sempre extraído da URI (Path Variable) em obediência às boas práticas REST.
 */
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
