package org.code.api.dto.session.register.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) que encapsula o payload para o login de um usuário.
 *
 * <p>Implementado como um {@code record} para garantir imutabilidade absoluta dos dados de
 * entrada. Este objeto atua como o contrato da API com o mundo exterior (Front-end/Mobile),
 * garantindo que apenas propriedades estritamente necessárias para a criação cheguem à
 * camada de domínio.
 *
 *
 * @param nome Nome completo do usuário
 * @param email E-mail válido
 * @param senha Senha forte de acesso
 *
 * @implNote Não expõe campos de controle interno como {@code id}, {@code ativo} ou
 * {@code createdBy}. O status inicial e a autoria são definidos exclusivamente pelas
 * regras de negócio dentro do {@link org.code.api.services.VehicleService}.
 */
@Schema(description = "Payload para registro de um novo usuário no sistema")
public record RegisterRequestDTO(

        @Schema(description = "Nome completo do usuário", example = "João da Silva")
        @NotBlank(message = "{api.user.nome.notblank}")
        @Size(min = 3, max = 100, message = "{api.user.nome.size}")
        String nome,

        @Schema(description = "E-mail válido", example = "joao.silva@empresa.com")
        @NotBlank(message = "{api.user.email.notblank}")
        @Email(message = "{api.user.email.invalid}")
        String email,

        @Schema(description = "Senha forte de acesso", example = "S3nh@F0rt3!")
        @NotBlank(message = "{api.user.senha.notblank}")
        @Size(min = 8, max = 72, message = "{api.user.senha.size}")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]+$",
                message = "{api.user.senha.fraca}"
        )
        String senha


) {
    @Override
    public String toString() {
        return "RegisterRequestDTO[nome=" + nome + ", email=" + email + ", senha=***MASKED***]";
    }
}
