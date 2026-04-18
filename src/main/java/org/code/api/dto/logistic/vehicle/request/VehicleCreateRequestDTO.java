package org.code.api.dto.logistic.vehicle.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) que encapsula o payload para a criação de um novo Veículo.
 *
 * <p>Implementado como um {@code record} para garantir imutabilidade absoluta dos dados de
 * entrada. Este objeto atua como o contrato da API com o mundo exterior (Front-end/Mobile),
 * garantindo que apenas propriedades estritamente necessárias para a criação cheguem à
 * camada de domínio.
 *
 * @param placa A placa identificadora do veículo (deve ser validada quanto ao formato e unicidade).
 * @param modelo A descrição ou nome do modelo do veículo.
 *
 * @implNote Não expõe campos de controle interno como {@code id}, {@code ativo} ou
 * {@code createdBy}. O status inicial e a autoria são definidos exclusivamente pelas
 * regras de negócio dentro do {@link org.code.api.services.VehicleService}.
 */
public record VehicleCreateRequestDTO(
    @NotBlank(message = "Placa é um campo obrigatório")
    @Size(max = 191, message = "Placa deve ter no máximo 191 caracteres")
    String placa,
    @NotBlank(message = "Modelo é um campo obrigatório")
    @Size(max = 191, message = "Modelo deve ter no máximo 191 caracteres")
    String modelo
) {}
