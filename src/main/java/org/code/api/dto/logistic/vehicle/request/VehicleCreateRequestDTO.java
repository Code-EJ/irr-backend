package org.code.api.dto.logistic.vehicle.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
@Schema(description = "Payload para cadastro de um novo veículo na frota")
public record VehicleCreateRequestDTO(
        @Schema(description = "Placa do veículo (Mercosul ou padrão antigo)", example = "ABC1D23")
        @NotBlank(message = "{api.vehicle.placa.notblank}")
        @Pattern(regexp = "^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$", message = "{api.vehicle.placa.pattern}")
        String placa,

        @Schema(description = "Modelo/Marca do veículo", example = "Toyota Corolla")
        @NotBlank(message = "{api.vehicle.modelo.notblank}")
    @Size(max = 191, message = "{api.vehicle.modelo.size}")
    String modelo
) {}
